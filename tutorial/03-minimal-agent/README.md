# 第 03 节：最小可用 Agent

## 本节目标

实现一个最小但可用的 ReactAgent — 能接收用户输入、调用 LLM、返回文本结果。

## 本节新增

- ✅ `ReactAgent` — Builder 模式，最简版本（chatModel + instructions + call）
- ✅ `AgentLoopExecutor` — 单轮 LLM 调用（无工具、无多轮）
- ✅ `LoopMessageBuilder` — 构建 SystemMessage + UserMessage

## 上节回顾

第 02 节定义了核心数据模型（AgentResult、AgentStreamEvent、RunnableParams 等）。
本节基于这些模型，实现第一个能工作的 Agent。

## 设计思路

### ReactAgent 的渐进式构建

完整版的 ReactAgent 有 20+ 个属性。本节只引入最核心的 3 个：

```
第 03 节（本节）: chatModel + instructions + call()
第 04 节: + maxRounds + AgentLoopExecutor 多轮循环
第 05 节: + tools + 工具调用
第 06 节: + stream() 流式输出
第 07 节: + sessionMessageStore + 会话持久化
...
第 15 节: 全部 20+ 属性
```

### 核心架构

```
用户代码
  │
  ▼
ReactAgent.call("你好")
  │
  ├── LoopMessageBuilder.buildMessages()
  │     ├── SystemMessage(instructions)
  │     └── UserMessage("你好")
  │
  ├── AgentLoopExecutor.call(messages)
  │     └── ChatClient.prompt().messages(messages).call()
  │
  └── 返回 AgentResult.Completed(answer)
```

### 为什么用 Builder 模式？

Agent 配置项多（chatModel、instructions、tools、hooks...），用 Builder 可以：
1. **可读性**：`.chatModel(m).instructions("...").tools(t).build()` 一目了然
2. **可选参数**：不需要每个参数都传，有合理默认值
3. **不可变**：build() 后返回的对象所有字段都是 final

### ChatClient vs ChatModel

```
ChatModel — 底层 LLM 调用接口
  └── chatModel.call(prompt) → ChatResponse

ChatClient — 高级封装，支持 Advisor chain、工具配置
  └── chatClient.prompt().messages(msgs).call() → String
```

**我们选择 ChatClient**，因为后续需要：
- Advisor chain（PauseAdvisor、RequestLoggingAdvisor）
- 工具配置（ToolCallingChatOptions）
- 流式调用

## 代码实现

### 1. LoopMessageBuilder — 消息构建器

```java
public class LoopMessageBuilder {
    private final String instructions;

    public List<Message> buildMessages(String query) {
        List<Message> messages = new ArrayList<>();
        // 1. 系统提示词
        if (instructions != null && !instructions.isBlank()) {
            messages.add(new SystemMessage(instructions));
        }
        // 2. 用户消息
        messages.add(new UserMessage(query));
        return messages;
    }
}
```

**职责：** 将 instructions + query 转换为 Spring AI 的 Message 列表。

### 2. AgentLoopExecutor — 循环执行器

```java
public class AgentLoopExecutor {
    private final ChatClient chatClient;

    public AgentResult call(String query, RunnableParams params) {
        // 1. 构建消息
        List<Message> messages = messageBuilder.buildMessages(query);
        // 2. 调用 LLM
        String answer = chatClient.prompt()
            .messages(messages)
            .call()
            .content();
        // 3. 返回结果
        return new AgentResult.Completed(answer);
    }
}
```

**本节简化：** 只调用一次 LLM，不处理工具调用，不循环。

### 3. ReactAgent — 对外入口

```java
public class ReactAgent {
    private final ChatModel chatModel;
    private final String instructions;

    // 核心方法
    public String call(String query) {
        return createExecutor().call(query, RunnableParams.empty());
    }

    // 每次调用创建新的 executor（线程安全）
    private AgentLoopExecutor createExecutor() {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        return new AgentLoopExecutor(chatClient, new LoopMessageBuilder(instructions));
    }

    // Builder 模式
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private ChatModel chatModel;
        private String instructions;

        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
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

```java
// 配置 ChatModel（以 OpenAI 为例）
OpenAiChatModel chatModel = new OpenAiChatModel(
    new OpenAiApi("sk-xxx"));

// 构建 Agent
ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .instructions("你是一个有帮助的助手。")
    .build();

// 调用
String answer = agent.call("你好，请介绍一下自己");
System.out.println(answer);
```

## 与原始项目的对比

| 特性 | 本节实现 | 原始项目 |
|------|---------|---------|
| ReactAgent 属性 | 2 个 (chatModel, instructions) | 20+ 个 |
| LLM 调用次数 | 1 次 | 多轮循环 |
| 工具调用 | ❌ | ✅ |
| 流式输出 | ❌ | ✅ |
| 会话持久化 | ❌ | ✅ |

## 编译验证

```bash
mvn clean compile -pl spring-ai-agentx-core
```

## 本节小结

✅ 实现了最小可用的 ReactAgent
✅ 理解 Builder 模式在复杂配置场景中的应用
✅ 理解 ChatClient vs ChatModel 的区别
✅ 理解消息构建的职责分离

## 下节预告

**第 04 节：ReAct 循环引擎**

将实现：
- 多轮循环：LLM 返回工具调用 → 执行工具 → 将结果加入消息 → 再次调用 LLM
- maxRounds 限制：防止无限循环
- 工具调用检测：判断 LLM 是否返回了 tool_calls

核心变化：AgentLoopExecutor 从「调用一次」变成「循环调用直到没有工具调用」。
