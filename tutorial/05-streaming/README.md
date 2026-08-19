# 第 05 节：流式输出

## 本节目标

实现 Flux 流式输出：Agent 逐 token 发射文本事件，客户端实时接收。

## 本节新增

- ✅ `AgentLoopExecutor.stream()` — 基于 Reactor Sink 的流式执行
- ✅ `ReactAgent.stream()` — 返回 `Flux<String>` 文本流
- ✅ `AgentStreamEvent.Thinking` — 思考事件（为第 09 节铺垫）
- ✅ `RoundState` — 轮次状态（累积 chunk 的缓冲区）
- ✅ `AgentStreamEvent.Complete` 增加 conversationId/sessionId 字段

## 核心设计

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
- 上游：LLM 流式响应 → processChunk → sink.tryEmitNext
- 下游：sink.asFlux() → 调用方订阅

### 关键挑战

1. **工具调用 chunk 合并**：流式 tool_calls 分多个 chunk 到达，需按 id 合并 arguments
2. **轮次边界**：一轮完成后再触发工具执行，然后开始下一轮
3. **终态信号**：无工具调用 → emit Complete → tryEmitComplete

## 使用示例

```java
// 流式调用
agent.stream("写一首关于春天的诗")
    .doOnNext(System.out::print)
    .blockLast();

// 完整事件流
agent.streamForResult("你好", RunnableParams.empty())
    .doOnNext(event -> {
        switch (event) {
            case AgentStreamEvent.Text t -> System.out.print(t.content());
            case AgentStreamEvent.ToolStart ts -> System.out.println("[工具] " + ts.toolName());
            case AgentStreamEvent.Complete c -> System.out.println("[完成]");
        }
    })
    .blockLast();
```

## 本节小结

✅ 理解 Reactor Sink 在流式 Agent 中的作用
✅ 实现流式 chunk 合并和轮次控制
✅ 区分 `stream(query)` 和 `streamForResult(query)`
