# 第 04 节：ReAct 循环引擎

> 📚 **这是 Spring AI AgentX 渐进式复现教程的第 04 节**  
> 本教程共 15 节，通过独立 git 分支逐步叠加 feature，最终 1:1 复现完整的 Java Agent 框架。  
> 👉 [查看教程目录](../..) | [← 上一节](../03-minimal-agent/README.md) | [下一节 →](../05-streaming/README.md)

## 本节目标

实现 ReAct（Reasoning + Acting）多轮循环：LLM 调用 → 检测工具调用 → 执行工具 → 将结果加入消息 → 再次调用 LLM → 直到没有工具调用或达到上限。

## 本节新增

- ✅ `AgentLoopExecutor` 升级 — 多轮循环 + 工具调用检测 + 工具执行
- ✅ `ToolCallExecutor` — 工具执行器（执行工具、组装消息）
- ✅ `ReactAgent` 新增 `maxRounds` 和 `tools` 属性
- ✅ `PendingToolCall` — 待处理的工具调用记录

## 上节回顾

第 03 节的 AgentLoopExecutor 只调用一次 LLM 就返回。本节升级为多轮循环。

## 核心设计

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

1. **maxRounds**：防止无限循环，默认 100
2. **internalToolExecutionEnabled(false)**：工具执行由框架控制，不由 ChatClient 自动执行
3. **工具消息格式**：ToolResponseMessage 必须与 ToolCall 一一对应

## 代码实现

### 核心变更：AgentLoopExecutor

```java
public AgentResult call(String query, RunnableParams params) {
    List<Message> messages = messageBuilder.buildMessages(query);
    
    for (int round = 0; round < maxRounds; round++) {
        // 调用 LLM
        ChatResponse response = chatClient.prompt()
            .messages(messages).call().chatResponse();
        
        AssistantMessage assistant = response.getResult().getOutput();
        
        // 无工具调用 → 返回最终答案
        if (assistant.getToolCalls().isEmpty()) {
            messages.add(assistant);
            return new AgentResult.Completed(assistant.getText());
        }
        
        // 有工具调用 → 添加 assistant 消息 + 执行工具
        messages.add(assistant);
        for (ToolCall tc : assistant.getToolCalls()) {
            String result = toolCallExecutor.execute(tc);
            messages.add(buildToolResponse(tc, result));
        }
        // 继续下一轮
    }
    
    // 达到上限
    return new AgentResult.Completed("达到最大轮次限制");
}
```

## 使用示例

```java
ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .instructions("你是一个有帮助的助手，可以查询天气。")
    .maxRounds(10)
    .tools(weatherTool)  // 注册天气查询工具
    .build();

String answer = agent.call("北京今天天气怎么样？");
// LLM 调用 get_weather 工具 → 获得结果 → 生成最终回答
```

## 与原始项目的对比

| 特性 | 本节实现 | 原始项目 |
|------|---------|---------|
| 循环控制 | for 循环 | scheduleRound 异步递归 |
| 工具执行 | 同步顺序 | 并发执行 (boundedElastic) |
| 错误处理 | 基本 try-catch | 重试 + Hook + 审计 |
| 流式输出 | ❌ | ✅ (第 06 节) |

## 本节小结

✅ 实现 ReAct 多轮循环
✅ 理解工具调用的消息格式
✅ 理解 maxRounds 的防循环作用
