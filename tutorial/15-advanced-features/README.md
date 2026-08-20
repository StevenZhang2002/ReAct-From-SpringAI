# 第 15 节：高级特性（SubAgent + ToolSearch）

## 本节目标

实现高级特性：子代理（SubAgent）和工具搜索（ToolSearch），让 Agent 能够委派任务给子代理，并动态搜索和加载工具。

## 上节回顾

第 14 节实现了高级内置工具（TodoWrite）。但 Agent 还缺乏任务委派和动态工具发现的能力。

## 本节新增

- ✅ `SubAgentTool` — 子代理工具（将 Agent 包装为工具）
- ✅ `ToolSearchTool` — 工具搜索元工具（按需搜索和加载工具）

## 核心设计

### SubAgentTool

将 ReactAgent 包装为 ToolCallback，实现任务委派。

```java
// 创建子代理工具
ToolCallback subAgent = SubAgentTool.create(
    "code_reviewer",                    // 子代理名称
    "代码审查专家，负责审查代码质量",      // 描述
    () -> createCodeReviewerAgent()     // 工厂方法
);

// 主 Agent 可以委派任务
ReactAgent mainAgent = ReactAgent.builder()
    .chatModel(chatModel)
    .tools(subAgent)
    .build();

// LLM 可以调用 call_code_reviewer 工具
mainAgent.call("请审查这段代码：...");
```

**子代理约束：**
- 禁止嵌套使用 SubAgentTool
- 禁止使用 AskUserTool
- 禁止使用 PauseAdvisor

### ToolSearchTool

让 LLM 按需搜索并加载 deferred 工具。

```java
// 配置延迟工具池
Map<String, ToolCallback> deferredPool = Map.of(
    "sql_query", sqlQueryTool,
    "http_request", httpRequestTool,
    "image_gen", imageGenTool
);

// 创建工具搜索
ToolCallback toolSearch = ToolSearchTool.create(
    config,
    deferredPool,
    discoveredNames,
    chatModel
);

// Agent 可以动态搜索工具
ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .tools(toolSearch)  // 始终加载
    .build();

// LLM 可以调用 tool_search 搜索更多工具
agent.call("帮我查询数据库中的用户数据");
// LLM 会先调用 tool_search("数据库查询")，然后调用 sql_query
```

**搜索模式：**
- `KEYWORD` — 关键词匹配（Jieba 分词 + 打分）
- `LLM` — LLM 选择
- `HYBRID` — 混合模式

### 使用示例

```java
// 创建子代理
ToolCallback codeReviewer = SubAgentTool.create(
    "code_reviewer",
    "代码审查专家",
    () -> ReactAgent.builder()
        .chatModel(chatModel)
        .instructions("你是一个代码审查专家...")
        .build()
);

// 创建工具搜索
ToolSearchConfig config = ToolSearchConfig.builder()
    .mode(ToolSearchConfig.Mode.HYBRID)
    .maxResults(3)
    .build();

ToolCallback toolSearch = ToolSearchTool.create(
    config,
    deferredPool,
    new HashSet<>(),
    chatModel
);

// 主 Agent
ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .tools(codeReviewer, toolSearch)
    .build();
```

## 与上节对比

| 维度 | 第 14 节 | 第 15 节 |
|------|---------|---------|
| 任务委派 | 无 | + SubAgent |
| 工具发现 | 静态 | + 动态搜索 |
| 复杂性 | 单 Agent | + 多 Agent 协作 |

## 教程总结

恭喜你完成了整个 Spring AI AgentX 渐进式复现教程！

### 教程回顾

| 节 | 主题 | 核心 Feature |
|---|------|-------------|
| 01 | 项目骨架 | Maven 多模块 |
| 02 | 核心模型 | ReactAgent, AgentResult, RunnableParams |
| 03 | 同步调用 | ReAct 循环基础 |
| 04 | ReAct 循环 | 工具调用、消息构建 |
| 05 | 流式输出 | Flux/Sink 流式模型 |
| 06 | 会话持久化 | SessionMessageStore, SessionPersister |
| 07 | 动态参数 | toolParams 注入, AgentTaskManager |
| 08 | 思考模型 | ThinkingMode, ThinkTagParser |
| 09 | HITL | PauseState, AskUserTool, resume |
| 10 | Hook | AgentHook, HookManager, 事件系统 |
| 11 | 上下文压缩 | ContextPolicy, ContextCompactor |
| 12 | 长期记忆 | LongTermMemoryManager, VectorStore |
| 13 | 追踪审计 | TraceStore, TraceManager |
| 14 | 高级工具 | TodoWriteTool |
| 15 | 高级特性 | SubAgentTool, ToolSearchTool |

### ReactAgent 属性增长

```
第03节: chatModel, instructions
第04节: + maxRounds, tools
第05节: + stream() 方法
第06节: + dataSource, enableSession
第07节: + taskManager
第08节: + thinkingMode
第09节: + askUser
第10节: + hooks
第11节: + contextPolicy
第12节: + longTermMemory
第13节: + enableTrace
```

## 本节小结

✅ 理解 SubAgent 任务委派模式
✅ 掌握 ToolSearch 动态工具发现
✅ 完成整个教程的学习
