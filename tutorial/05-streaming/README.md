# 第 05 节：流式输出

> 📚 **这是 Spring AI AgentX 渐进式复现教程的第 05 节**  
> 本教程共 15 节，通过独立 git 分支逐步叠加 feature，最终 1:1 复现完整的 Java Agent 框架。  
> 👉 [查看教程目录](../..) | [← 上一节](../04-react-loop/README.md) | [下一节 →](../06-session-persistence/README.md)

## 本节目标

实现 Flux 流式输出：Agent 逐 token 发射文本事件，客户端实时接收。这是构建实时交互 Agent 的关键能力。

## 上节回顾

第 04 节实现了 ReAct 多轮循环，Agent 可以调用工具并迭代推理。但所有输出都是同步阻塞的——用户必须等待 Agent 完全执行完毕才能看到结果。

本节将实现流式输出，让用户可以实时看到 Agent 的思考过程和回答。

## 本节新增

- ✅ `AgentLoopExecutor.stream()` — 基于 Reactor Sink 的流式执行
- ✅ `ReactAgent.stream()` — 返回 `Flux<String>` 文本流
- ✅ `ReactAgent.streamForResult()` — 返回 `Flux<AgentStreamEvent>` 完整事件流
- ✅ `AgentStreamEvent.Thinking` — 思考事件（为第 08 节铺垫）
- ✅ `RoundState` — 轮次状态（累积 chunk 的缓冲区）
- ✅ `RoundMode` — 轮次模式枚举（INIT / STREAMING / TOOL_CALL）
- ✅ `AgentStreamEvent.Complete` 增加 conversationId/sessionId 字段

## 核心设计

### 为什么需要流式输出？

```
同步调用（第 04 节）：
用户提问 → 等待 5-10 秒 → 一次性返回完整答案
                          ↑ 用户体验差，无法实时反馈

流式调用（本节）：
用户提问 → 0.5 秒后开始逐字返回 → 实时看到回答过程
           ↑ 用户体验好，可提前终止
```

### 流式架构

```
chatClient.prompt().stream()
    → Flux<ChatResponse>
    → processChunk() 逐个处理
    → sink.tryEmitNext(Text/ToolStart/ToolEnd)
    → Flux<AgentStreamEvent> 返回给调用方
```

### Sink 模式

使用 `Sinks.Many<AgentStreamEvent>` 作为事件桥梁：

```java
// 创建 Sink
Sinks.Many<AgentStreamEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

// 上游：LLM 流式响应 → processChunk → sink.tryEmitNext
chatClient.prompt().stream()
    .doOnNext(chunk -> processChunk(chunk, sink))
    .doOnComplete(() -> sink.tryEmitComplete())
    .subscribe();

// 下游：sink.asFlux() → 调用方订阅
return sink.asFlux();
```

**为什么用 Sink 而不是直接返回 Flux？**
- 需要在流式过程中插入非 LLM 事件（ToolStart、ToolEnd）
- 需要控制流的完成时机
- 需要处理多轮循环的流式拼接

### 关键挑战

#### 1. 工具调用 chunk 合并

流式 tool_calls 分多个 chunk 到达，需按 id 合并 arguments：

```
chunk1: {"tool_calls": [{"id": "call_1", "function": {"name": "get_weather", "arguments": "{\"lo"}}]}
chunk2: {"tool_calls": [{"id": "call_1", "function": {"arguments": "cation\": \"北京\"}"}}]}

合并后：{"id": "call_1", "function": {"name": "get_weather", "arguments": "{\"location\": \"北京\"}"}}
```

**实现：** 使用 `RoundState` 中的 `toolCallBuffer` 按 id 累积 arguments。

#### 2. 轮次边界

一轮完成后再触发工具执行，然后开始下一轮：

```
Round 1: 流式接收 → 检测到工具调用 → 执行工具 → 发射 ToolStart/ToolEnd
Round 2: 流式接收 → 无工具调用 → 发射 Complete → 结束
```

**实现：** 使用 `RoundMode` 状态机控制轮次转换。

#### 3. 终态信号

无工具调用 → emit Complete → tryEmitComplete：

```java
if (assistant.getToolCalls().isEmpty()) {
    sink.tryEmitNext(new AgentStreamEvent.Complete(promptTokens, completionTokens));
    sink.tryEmitComplete();
}
```

## 代码实现

### 1. RoundState — 轮次状态

```java
public class RoundState {
    private final StringBuilder textBuffer = new StringBuilder();      // 文本缓冲
    private final Map<String, StringBuilder> toolCallBuffer = new HashMap<>();  // 工具调用缓冲
    private final Map<String, ToolCall> toolCalls = new HashMap<>();   // 完整工具调用
    private int promptTokens = 0;
    private int completionTokens = 0;
    
    // 累积 chunk
    public void accumulate(AssistantMessage message) {
        if (message.getText() != null) {
            textBuffer.append(message.getText());
        }
        if (message.getToolCalls() != null) {
            for (ToolCall tc : message.getToolCalls()) {
                toolCallBuffer.computeIfAbsent(tc.id(), k -> new StringBuilder())
                    .append(tc.arguments());
                toolCalls.put(tc.id(), tc);
            }
        }
    }
}
```

### 2. AgentLoopExecutor.stream()

```java
public Flux<AgentStreamEvent> stream(String query, RunnableParams params) {
    Sinks.Many<AgentStreamEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
    
    List<Message> messages = messageBuilder.buildMessages(query);
    
    // 异步执行流式循环
    Schedulers.boundedElastic().schedule(() -> streamRound(messages, sink, 0));
    
    return sink.asFlux();
}

private void streamRound(List<Message> messages, Sinks.Many<AgentStreamEvent> sink, int round) {
    if (round >= maxRounds) {
        sink.tryEmitComplete();
        return;
    }
    
    RoundState state = new RoundState();
    
    chatClient.prompt()
        .messages(messages)
        .stream()
        .doOnNext(chunk -> {
            state.accumulate(chunk);
            // 发射文本事件
            if (chunk.getText() != null && !chunk.getText().isEmpty()) {
                sink.tryEmitNext(new AgentStreamEvent.Text(chunk.getText()));
            }
        })
        .doOnComplete(() -> {
            // 检查是否有工具调用
            if (state.getToolCalls().isEmpty()) {
                // 无工具调用 → 完成
                sink.tryEmitNext(new AgentStreamEvent.Complete(
                    state.getPromptTokens(), state.getCompletionTokens()));
                sink.tryEmitComplete();
            } else {
                // 有工具调用 → 执行工具 → 下一轮
                messages.add(new AssistantMessage(state.getTextBuffer().toString(), 
                    Map.of(), state.getToolCalls().values().stream().toList()));
                
                for (ToolCall tc : state.getToolCalls().values()) {
                    sink.tryEmitNext(new AgentStreamEvent.ToolStart(
                        tc.name(), tc.id(), tc.arguments()));
                    
                    String result = toolCallExecutor.execute(tc);
                    
                    sink.tryEmitNext(new AgentStreamEvent.ToolEnd(
                        tc.name(), tc.id(), result));
                    
                    messages.add(buildToolResponse(tc, result));
                }
                
                // 递归进入下一轮
                streamRound(messages, sink, round + 1);
            }
        })
        .subscribe();
}
```

### 3. ReactAgent — 流式 API

```java
public class ReactAgent {
    // 同步调用（继承自第 04 节）
    public String call(String query) { ... }
    public AgentResult callForResult(String query, RunnableParams params) { ... }
    
    // 流式调用（本节新增）
    
    /**
     * 流式调用，返回文本流。
     */
    public Flux<String> stream(String query) {
        return stream(query, RunnableParams.empty());
    }
    
    /**
     * 流式调用（带参数），返回文本流。
     */
    public Flux<String> stream(String query, RunnableParams params) {
        return createExecutor().stream(query, params)
            .filter(e -> e instanceof AgentStreamEvent.Text)
            .map(e -> ((AgentStreamEvent.Text) e).content());
    }
    
    /**
     * 流式调用，返回完整事件流。
     */
    public Flux<AgentStreamEvent> streamForResult(String query, RunnableParams params) {
        return createExecutor().stream(query, params);
    }
}
```

## 使用示例

### 简单文本流

```java
ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .instructions("你是一个有帮助的助手")
    .maxRounds(10)
    .build();

// 流式输出到控制台
agent.stream("写一首关于春天的诗")
    .doOnNext(System.out::print)
    .blockLast();

// 输出：
// 春风轻拂柳丝长，
// 桃李芬芳满园香。
// ...
```

### 完整事件流

```java
// 获取完整事件流（包含工具调用信息）
agent.streamForResult("北京今天天气怎么样？", RunnableParams.empty())
    .doOnNext(event -> {
        switch (event) {
            case AgentStreamEvent.Text t -> 
                System.out.print(t.content());
            case AgentStreamEvent.ToolStart ts -> 
                System.out.println("[工具开始] " + ts.toolName() + "(" + ts.arguments() + ")");
            case AgentStreamEvent.ToolEnd te -> 
                System.out.println("[工具结束] " + te.toolName() + " → " + te.result());
            case AgentStreamEvent.Complete c -> 
                System.out.println("\n[完成] tokens: " + c.totalPromptTokens() + "+" + c.totalCompletionTokens());
            default -> {}
        }
    })
    .blockLast();

// 输出：
// [工具开始] get_weather({"location": "北京"})
// [工具结束] get_weather → 晴天, 25°C
// 北京今天晴天，气温 25°C。
// [完成] tokens: 50+20
```

### 流式输出到 Web 客户端

```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(@RequestParam String query) {
    return agent.stream(query);
}

// 前端使用 EventSource 接收：
// const eventSource = new EventSource('/chat/stream?query=你好');
// eventSource.onmessage = (event) => {
//     console.log(event.data);  // 逐字输出
// };
```

## 与上节对比

| 维度 | 第 04 节 | 第 05 节 |
|------|---------|---------|
| 输出方式 | 同步阻塞 | 异步流式 |
| 返回类型 | `String` / `AgentResult` | `Flux<String>` / `Flux<AgentStreamEvent>` |
| 用户体验 | 等待 5-10 秒 | 实时逐字输出 |
| 工具调用 | 同步执行 | 流式中插入 ToolStart/ToolEnd 事件 |
| 新增类 | — | RoundState, RoundMode |
| ReactAgent 属性 | chatModel, instructions, maxRounds, tools | 相同（新增 stream() 方法） |

## ReactAgent 属性增长

```
第03节: chatModel, instructions
第04节: + maxRounds, tools
第05节: + stream() 方法  ← 本节
```

## 与原始项目的对比

| 特性 | 本节实现 | 原始项目 |
|------|---------|---------|
| 流式架构 | Sink 模式 | 相同 |
| 工具调用合并 | 按 id 累积 arguments | 相同 |
| 轮次控制 | RoundMode 状态机 | 相同 |
| 事件类型 | Text/ToolStart/ToolEnd/Complete | + Thinking（第 08 节） |
| 并发控制 | 无 | + AgentTaskManager（第 07 节） |

## 编译验证

```bash
mvn clean compile -pl spring-ai-agentx-core
```

## 本节小结

✅ 理解 Reactor Sink 在流式 Agent 中的作用  
✅ 实现流式 chunk 合并和轮次控制  
✅ 区分 `stream(query)` 和 `streamForResult(query)`  
✅ 掌握工具调用在流式场景下的处理方式  
✅ 能够将流式输出集成到 Web 应用  

## 下节预告

**第 06 节：会话持久化**

将实现：
- `SessionMessageStore` — 会话消息存储（agentx_session 表）
- `ConversationStore` — 会话窗口记录（agentx_conversation 表）
- `MessageJsonSerializer` — Message ↔ JSON 双向序列化
- `SessionPersister` — 持久化入口

目标：让 Agent 拥有「记忆」，能够记住多轮对话内容。
