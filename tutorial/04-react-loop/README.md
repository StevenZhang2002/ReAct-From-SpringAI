# 第 04 节：ReAct 循环引擎

> 📚 **这是 Spring AI AgentX 渐进式复现教程的第 04 节**  
> 本教程共 15 节，通过独立 git 分支逐步叠加 feature，最终 1:1 复现完整的 Java Agent 框架。  
> 👉 [查看教程目录](../..) | [← 上一节](../03-minimal-agent/README.md) | [下一节 →](../05-streaming/README.md)

## 本节目标

实现 ReAct（Reasoning + Acting）多轮循环：LLM 调用 → 检测工具调用 → 执行工具 → 将结果加入消息 → 再次调用 LLM → 直到没有工具调用或达到上限。

这是 Agent 框架的核心——让 LLM 能够「思考-行动-观察」循环迭代，最终完成复杂任务。

## 上节回顾

第 03 节实现了最小可用 Agent，但只能单轮调用 LLM。如果 LLM 需要调用工具（如查询天气、搜索信息），就无法处理。

本节将 AgentLoopExecutor 从「调用一次」升级为「循环调用直到没有工具调用」。

## 本节新增

- ✅ `AgentLoopExecutor` 升级 — 多轮循环 + 工具调用检测 + 工具执行
- ✅ `ToolCallExecutor` — 工具执行器（执行工具、组装消息、参数校验）
- ✅ `ReactAgent` 新增 `maxRounds` 和 `tools` 属性
- ✅ `PendingToolCall` — 待处理的工具调用记录

## 核心设计

### 什么是 ReAct？

ReAct = **Re**asoning + **Act**ing，是一种让 LLM 交替进行推理和行动的范式：

```
用户：北京今天天气怎么样？

Round 1（推理）：
LLM 思考：用户想知道北京天气，我需要调用 get_weather 工具
LLM 行动：调用 get_weather({"location": "北京"})

Round 1（观察）：
工具返回：晴天, 25°C

Round 2（推理）：
LLM 思考：我已经获得天气信息，可以回答用户了
LLM 行动：无工具调用，直接回答

最终回答：北京今天晴天，气温 25°C。
```

### ReAct 循环流程

```
Round 1: UserMessage → LLM → AssistantMessage(tool_calls=[get_weather])
                                    ↓
                              执行工具 get_weather("北京")
                                    ↓
                              ToolResponseMessage("晴天, 25°C")
                                    ↓
Round 2: messages → LLM → AssistantMessage("北京今天晴天, 25°C")
                                    ↓
                              无工具调用 → 返回最终答案
```

### 关键约束

#### 1. maxRounds — 防止无限循环

```java
for (int round = 1; round <= maxRounds; round++) {
    // 调用 LLM
    // 检查工具调用
    // 执行工具
}
// 达到最大轮次 → 返回失败
```

**为什么需要 maxRounds？**
- LLM 可能陷入死循环（反复调用同一工具）
- 防止资源耗尽（CPU、内存、API 调用费用）
- 默认值 100，可根据场景调整

#### 2. internalToolExecutionEnabled(false)

```java
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultOptions(ToolCallingChatOptions.builder()
        .internalToolExecutionEnabled(false)  // 禁用自动工具执行
        .build())
    .build();
```

**为什么禁用自动执行？**
- Spring AI 默认会自动执行工具调用（ChatClient 内部）
- 我们需要手动控制工具执行流程（插入 Hook、参数替换、审计等）
- 设置为 false 后，LLM 返回 tool_calls 时不会自动执行，由框架接管

#### 3. 工具消息格式

```java
// LLM 返回的工具调用
AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
    "call_1",           // id
    "get_weather",      // name
    "{\"location\": \"北京\"}"  // arguments (JSON)
);

// 工具执行结果必须包装为 ToolResponseMessage
ToolResponseMessage response = new ToolResponseMessage(
    List.of(new ToolResponse(
        "call_1",        // toolCallId（必须与 ToolCall.id 对应）
        "get_weather",   // name
        "晴天, 25°C"     // result
    ))
);
```

**关键点：**
- `toolCallId` 必须与 `ToolCall.id` 一一对应
- LLM 通过 toolCallId 关联工具调用和结果
- 多个工具调用可以放在同一个 ToolResponseMessage 中

## 代码实现

### 1. AgentLoopExecutor — 多轮循环

```java
public class AgentLoopExecutor {
    private final ChatClient chatClient;
    private final LoopMessageBuilder messageBuilder;
    private final ToolCallExecutor toolCallExecutor;
    private final int maxRounds;
    
    public AgentResult call(String query, RunnableParams params) {
        // 1. 构建初始消息
        List<Message> messages = messageBuilder.buildMessages(query, params);
        
        // 2. 多轮循环
        for (int round = 1; round <= maxRounds; round++) {
            log.debug("Round {}/{}", round, maxRounds);
            
            // 调用 LLM
            ChatResponse response = chatClient.prompt()
                .messages(messages)
                .call()
                .chatResponse();
            
            AssistantMessage assistantMsg = response.getResult().getOutput();
            List<AssistantMessage.ToolCall> toolCalls = assistantMsg.getToolCalls();
            
            // 无工具调用 → 返回最终答案
            if (toolCalls == null || toolCalls.isEmpty()) {
                messages.add(assistantMsg);
                String answer = assistantMsg.getText();
                return new AgentResult.Completed(answer != null ? answer : "");
            }
            
            // 有工具调用 → 执行工具 → 添加结果消息
            messages.add(assistantMsg);
            for (AssistantMessage.ToolCall tc : toolCalls) {
                String result = toolCallExecutor.execute(tc, params);
                messages.add(toolCallExecutor.buildToolResponseMessage(tc, result));
            }
            // 继续下一轮
        }
        
        // 达到最大轮次
        return new AgentResult.Failed("达到最大轮次限制 (" + maxRounds + ")",
            AgentErrorCode.LLM_CALL_FAILED);
    }
}
```

### 2. ToolCallExecutor — 工具执行器

```java
public class ToolCallExecutor {
    private final Map<String, ToolCallback> toolMap;
    
    public ToolCallExecutor(List<ToolCallback> tools) {
        this.toolMap = new HashMap<>();
        if (tools != null) {
            for (ToolCallback tool : tools) {
                toolMap.put(tool.getToolDefinition().name(), tool);
            }
        }
    }
    
    public String execute(AssistantMessage.ToolCall toolCall, RunnableParams params) {
        String toolName = toolCall.name();
        String argsJson = toolCall.arguments();
        
        if (argsJson == null || argsJson.isBlank()) {
            argsJson = "{}";
        }
        
        ToolCallback callback = toolMap.get(toolName);
        if (callback == null) {
            log.warn("Tool not found: {}", toolName);
            return "{\"error\": \"工具 '" + toolName + "' 不存在\"}";
        }
        
        try {
            log.debug("Executing tool: {} with args: {}", toolName, argsJson);
            ToolContext toolContext = new ToolContext(new HashMap<>());
            Object result = callback.call(argsJson, toolContext);
            return result != null ? result.toString() : "{}";
        } catch (Exception e) {
            log.error("Tool '{}' execution failed: {}", toolName, e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    public ToolResponseMessage buildToolResponseMessage(
            AssistantMessage.ToolCall toolCall, String result) {
        ToolResponseMessage.ToolResponse tr = new ToolResponseMessage.ToolResponse(
            toolCall.id(), toolCall.name(), result);
        return new ToolResponseMessage(List.of(tr));
    }
}
```

### 3. ReactAgent — 新增属性

```java
public class ReactAgent {
    private final ChatModel chatModel;
    private final String instructions;
    private final int maxRounds;           // 本节新增
    private final List<ToolCallback> tools; // 本节新增
    
    private ReactAgent(Builder builder) {
        this.chatModel = builder.chatModel;
        this.instructions = builder.instructions;
        this.maxRounds = builder.maxRounds != null ? builder.maxRounds : 100;
        this.tools = List.copyOf(builder.tools);
    }
    
    private AgentLoopExecutor createExecutor() {
        ChatClient chatClient = ChatClient.builder(chatModel)
            .defaultOptions(ToolCallingChatOptions.builder()
                .toolCallbacks(tools)
                .internalToolExecutionEnabled(false)
                .build())
            .build();
        
        ToolCallExecutor toolCallExecutor = new ToolCallExecutor(tools);
        
        return new AgentLoopExecutor(
            chatClient,
            new LoopMessageBuilder(instructions),
            toolCallExecutor,
            maxRounds
        );
    }
    
    public static class Builder {
        private ChatModel chatModel;
        private String instructions;
        private Integer maxRounds;
        private List<ToolCallback> tools = new ArrayList<>();
        
        public Builder maxRounds(int maxRounds) {
            this.maxRounds = maxRounds;
            return this;
        }
        
        public Builder tools(ToolCallback... tools) {
            this.tools.addAll(Arrays.asList(tools));
            return this;
        }
        
        public ReactAgent build() {
            Objects.requireNonNull(chatModel, "chatModel must not be null");
            return new ReactAgent(this);
        }
    }
}
```

## 使用示例

### 基础工具调用

```java
// 定义天气查询工具
ToolCallback weatherTool = ToolCallback.builder()
    .name("get_weather")
    .description("查询指定城市的天气")
    .inputSchema("""
        {
            "type": "object",
            "properties": {
                "location": {
                    "type": "string",
                    "description": "城市名称"
                }
            },
            "required": ["location"]
        }
        """)
    .function(args -> {
        String location = JsonPath.read(args, "$.location");
        return "晴天, 25°C";  // 实际项目中调用天气 API
    })
    .build();

// 构建 Agent
ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .instructions("你是一个有帮助的助手，可以查询天气。")
    .maxRounds(10)
    .tools(weatherTool)
    .build();

// 调用
String answer = agent.call("北京今天天气怎么样？");
System.out.println(answer);  // 输出：北京今天晴天，气温 25°C。
```

### 多工具协作

```java
// 定义多个工具
ToolCallback weatherTool = ...;
ToolCallback searchTool = ...;
ToolCallback calculatorTool = ...;

ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .instructions("你是一个智能助手，可以查询天气、搜索信息和计算。")
    .maxRounds(10)
    .tools(weatherTool, searchTool, calculatorTool)
    .build();

// LLM 会根据任务自动选择合适的工具
agent.call("北京和上海今天天气哪个好？");
// LLM 可能调用 weatherTool 两次（北京、上海），然后比较结果
```

## 与上节对比

| 维度 | 第 03 节 | 第 04 节 |
|------|---------|---------|
| LLM 调用次数 | 1 次 | 多轮循环（最多 maxRounds 次） |
| 工具调用 | ❌ | ✅ |
| ReactAgent 属性 | chatModel, instructions | + maxRounds, tools |
| AgentLoopExecutor | 单轮调用 | 多轮循环 + 工具执行 |
| 新增类 | — | ToolCallExecutor, PendingToolCall |

## ReactAgent 属性增长

```
第03节: chatModel, instructions
第04节: + maxRounds, tools  ← 本节
```

## 与原始项目的对比

| 特性 | 本节实现 | 原始项目 |
|------|---------|---------|
| 循环控制 | for 循环 | scheduleRound 异步递归 |
| 工具执行 | 同步顺序 | 并发执行 (boundedElastic) |
| 错误处理 | 基本 try-catch | 重试 + Hook + 审计 |
| 参数替换 | 无 | + toolParams（第 07 节） |
| 流式输出 | ❌ | ✅ (第 05 节) |

## 编译验证

```bash
mvn clean compile -pl spring-ai-agentx-core
```

## 本节小结

✅ 实现 ReAct 多轮循环  
✅ 理解工具调用的消息格式（ToolCall → ToolResponseMessage）  
✅ 理解 maxRounds 的防循环作用  
✅ 掌握 internalToolExecutionEnabled(false) 的意义  
✅ 能够为 Agent 注册多个工具并自动选择  

## 下节预告

**第 05 节：流式输出**

将实现：
- `AgentLoopExecutor.stream()` — 基于 Reactor Sink 的流式执行
- `ReactAgent.stream()` — 返回 `Flux<String>` 文本流
- `RoundState` — 轮次状态（累积 chunk 的缓冲区）

目标：让用户可以实时看到 Agent 的思考过程和回答，而不是等待完整结果。
