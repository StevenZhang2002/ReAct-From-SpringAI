# 第 07 节：动态参数与并发控制

> 📚 **这是 Spring AI AgentX 渐进式复现教程的第 07 节**  
> 本教程共 15 节，通过独立 git 分支逐步叠加 feature，最终 1:1 复现完整的 Java Agent 框架。  
> 👉 [查看教程目录](../..) | [← 上一节](../06-session-persistence/README.md) | [下一节 →](../08-thinking-mode/README.md)

## 本节目标

实现两个关键能力：
1. **toolParams 动态注入** — 运行时参数（userId、token）安全注入工具调用，不依赖 LLM 生成
2. **AgentTaskManager** — 管理流式任务的生命周期，防止同一会话并发执行

## 上节回顾

第 06 节实现了会话持久化，Agent 可以记住多轮对话。但工具执行时，参数完全依赖 LLM 生成——
如果 LLM 编造 userId 或遗漏 token，工具就会出错。

## 本节新增

- ✅ `ToolCallExecutor.replaceToolParams()` — 按工具 inputSchema 过滤注入 toolParams
- ✅ `AgentTaskManager` — 任务注册/停止/清理（简化版，中断功能留给第 09 节）
- ✅ `ReactAgent` 新增 `taskManager` 属性
- ✅ `AgentLoopExecutor` 集成任务注册与清理

## 核心设计

### toolParams vs customParams

```
RunnableParams
├── customParams  → 注入 SystemMessage → LLM 可见
│   例: addParam("language", "zh-CN")
│
└── toolParams    → 工具执行前注入 → LLM 不可见
    例: addToolParam("userId", "123")
```

**为什么需要 toolParams？**

LLM 不可信。如果工具需要 userId，让 LLM 生成可能编造或遗漏。
toolParams 在工具执行前按 inputSchema 自动注入，确保安全。

### 注入流程

```
LLM 返回: {"sql": "SELECT * FROM users"}
toolParams: {"userId": "123"}

工具的 inputSchema:
{
  "type": "object",
  "properties": {
    "sql": {"type": "string"},
    "userId": {"type": "string"}
  }
}

注入后: {"sql": "SELECT * FROM users", "userId": "123"}
```

**关键**：只注入工具 inputSchema 中声明的字段，避免污染 MCP 等严格校验的工具。

### AgentTaskManager

```
conversationId → TaskInfo (sink, disposable)
                     ↓
              同一 conversationId 不能并发
              registerTask() 返回 null → 拒绝执行
              
              stopTask() → dispose + complete
              removeTask() → 正常清理
```

简化版只实现基础的注册/停止/清理。
完整的 interrupt（含 PauseState 快照）在第 09 节添加。

## 使用示例

```java
// toolParams 示例
ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .tools(mySqlTool)
    .build();

// 安全注入 userId，不依赖 LLM
agent.call("查询所有用户", RunnableParams.builder()
    .addToolParam("userId", "123")     // 工具会收到真实 userId
    .addToolParam("token", "abc-def")  // LLM 不知道 token 的存在
    .build());

// AgentTaskManager 示例
AgentTaskManager taskManager = new AgentTaskManager();

ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .taskManager(taskManager)
    .build();

// 流式调用时自动注册任务
agent.streamForResult("你好", RunnableParams.builder()
    .conversationId("conv-001")
    .build());

// 需要停止时
taskManager.stopTask("conv-001");
```

## 与上节对比

| 维度 | 第 06 节 | 第 07 节 |
|------|---------|---------|
| 工具参数 | 完全依赖 LLM 生成 | toolParams 安全注入 |
| 并发控制 | 无 | AgentTaskManager 防并发 |
| ReactAgent 属性 | + dataSource, enableSession | + taskManager |
| ToolCallExecutor | 简单执行 | + replaceToolParams |

## ReactAgent 属性增长

```
第03节: chatModel, instructions
第04节: + maxRounds, tools
第05节: + stream() 方法
第06节: + dataSource, enableSession
第07节: + taskManager  ← 本节
```

## 本节小结

✅ 理解 toolParams 与 customParams 的区别
✅ 掌握按 inputSchema 过滤注入的安全机制
✅ 实现 AgentTaskManager 的基本任务管理
✅ 了解流式任务的生命周期（注册 → 执行 → 清理）
