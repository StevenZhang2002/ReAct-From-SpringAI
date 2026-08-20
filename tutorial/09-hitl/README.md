# 第 09 节：Human-in-the-Loop

## 本节目标

实现人机交互（HITL）：Agent 可以在需要时暂停执行、向用户提问，收到回答后恢复继续。

## 上节回顾

第 08 节实现了思考模型适配。但 Agent 完全是「自动」的——无法在需要人类判断时暂停。

## 本节新增

- ✅ `PauseReason` — 暂停原因枚举（HITL_TOOL_REQUEST / USER_INTERRUPT）
- ✅ `PauseState` — 暂停状态快照（含消息列表、待处理工具调用）
- ✅ `AskUserTool` — 让 Agent 主动向用户提问的工具
- ✅ `AgentResult.Paused` — 新的结果类型（表示 Agent 已暂停）
- ✅ `ReactAgent` 新增 `askUser` / `resume()` 方法

## 核心设计

### HITL 流程

```
Agent 执行中
  │
  ├── LLM 调用 ask_user 工具
  │
  ▼
Agent 检测到 ask_user
  │
  ├── 不执行工具
  ├── 构建 PauseState（含问题内容）
  ├── 返回 AgentResult.Paused
  │
  ▼
调用方收到 Paused 结果
  │
  ├── 展示问题给用户
  ├── 收集用户回答
  │
  ▼
agent.resume(pauseState, answers)
  │
  ├── 将用户回答作为工具结果
  ├── 继续 ReAct 循环
```

### PauseState 快照

```
PauseState
├── messages        — 当前消息列表（恢复时继续）
├── pendingToolCalls — 待处理的工具调用（含 ask_user 的问题）
├── params          — 原始调用参数
├── query           — 原始用户提问
├── sessionId       — 会话 ID
└── reason          — 暂停原因
```

### AskUserTool 两种模式

| 模式 | 说明 | 适用场景 |
|------|------|---------|
| HITL 模式 | 配合 PauseAdvisor，不阻塞 | Web 服务 |
| CLI 模式 | Scanner 阻塞等待输入 | 命令行应用 |

## 使用示例

```java
// HITL 模式
ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .tools(AskUserTool.create())  // 注册 ask_user 工具
    .askUser(true)                // 启用 HITL 拦截
    .build();

AgentResult result = agent.callForResult("帮我推荐旅行目的地");

if (result instanceof AgentResult.Paused p) {
    PauseState state = p.state();
    // 展示问题给用户
    for (PendingToolCall tc : state.getPendingToolCalls()) {
        System.out.println("Agent 提问: " + tc.arguments());
    }
    // 收集用户回答后恢复
    agent.resume(state, Map.of(toolCallId, "我喜欢海岛"));
}

// CLI 模式（阻塞式）
ReactAgent cliAgent = ReactAgent.builder()
    .chatModel(chatModel)
    .tools(AskUserTool.createBlocking())  // 阻塞等待控制台输入
    .build();
```

## 与上节对比

| 维度 | 第 08 节 | 第 09 节 |
|------|---------|---------|
| 执行模式 | 全自动 | 可暂停/恢复 |
| AgentResult | Completed / Failed | + Paused |
| 新增类 | — | PauseState, PauseReason, AskUserTool |
| ReactAgent | + thinkingMode | + askUser, resume() |

## ReactAgent 属性增长

```
第03节: chatModel, instructions
第04节: + maxRounds, tools
第05节: + stream() 方法
第06节: + dataSource, enableSession
第07节: + taskManager
第08节: + thinkingMode
第09节: + askUser  ← 本节
```

## 本节小结

✅ 理解 HITL 的暂停/恢复模式
✅ 掌握 PauseState 快照的设计
✅ 实现 AskUserTool 的两种模式
✅ 能够通过 resume() 恢复暂停的 Agent
