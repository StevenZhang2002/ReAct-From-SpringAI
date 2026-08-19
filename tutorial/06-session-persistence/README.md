# 第 06 节：会话持久化

## 本节目标

让 Agent 拥有「记忆」：多轮对话的消息自动存入数据库，下次调用时加载历史上下文。

## 上节回顾

第 05 节实现了 Flux 流式输出，Agent 可以逐 token 返回结果。但每次调用都是「失忆」的——
无法感知之前的对话内容。

## 本节新增

- ✅ `SessionMessageStore` — 会话消息链存储（agentx_session 表 CRUD）
- ✅ `ConversationStore` — 会话窗口记录（agentx_conversation 表）
- ✅ `MessageJsonSerializer` — Message ↔ OpenAI JSON 双向序列化
- ✅ `SessionPersister` — 持久化入口（集中管理落库副作用）
- ✅ `ReactAgent` 新增 `dataSource` / `enableSession` 属性
- ✅ `AgentLoopExecutor` 在开局/终态自动触发持久化
- ✅ `pom.xml` 新增 `spring-jdbc` + `mybatis-plus` 依赖

## 核心设计

### 两张表的职责

```
agentx_conversation（会话窗口表）
┌──────────┬────────────┬───────────┬──────────┐
│ session  │ question   │ status    │ created  │
│ _id      │            │           │ _at      │
├──────────┼────────────┼───────────┼──────────┤
│ 1001     │ 你好       │ completed │ 10:00    │
│ 1002     │ 天气怎样   │ running   │ 10:05    │
└──────────┴────────────┴───────────┴──────────┘

agentx_session（消息链表）
┌──────────┬───────────────┬────────────┬───────────┐
│ session  │ state_key     │ item_index │ state_data│
│ _id      │               │            │ (JSON)    │
├──────────┼───────────────┼────────────┼───────────┤
│ 1001     │ original_msgs │ 0          │ {user...} │
│ 1001     │ original_msgs │ 1          │ {asst...} │
│ 1001     │ working_msgs  │ 0          │ {user...} │
│ 1001     │ working_msgs  │ 1          │ {asst...} │
└──────────┴───────────────┴────────────┴───────────┘
```

- **conversation**：每次调用一行，记录 question + status（running/completed/error）
- **session**：每条消息一行，按 `conversation_id + state_key` 聚合

### 两个 state_key 的区别

| state_key | 含义 | 写入时机 |
|-----------|------|---------|
| `original_messages` | 原始消息（不压缩） | 终态时追加本次新增的消息 |
| `working_messages` | 工作消息（可能被压缩） | 终态时覆盖写全部当前消息 |

### 持久化时机

```
call() / stream() 开始
  │
  ├── SessionPersister.initSession()
  │     → 生成 sessionId
  │     → conversationStore.saveStart() 写开局记录
  │     → 加载历史消息（conversationId 不为空时）
  │
  ▼
ReAct 循环（不碰 DB，纯内存操作）
  │
  ▼
终态（complete / error / cancel）
  │
  ├── SessionPersister.persistOnTerminal()
  │     → sessionMessageStore.appendMessages() 追加原始消息
  │     → sessionMessageStore.replaceMessages() 覆盖工作消息
  │     → conversationStore.updateStatus() 更新终态
  │
  ▼
返回结果
```

### MessageJsonSerializer

Spring AI 的 `Message` 接口有多种实现（UserMessage、AssistantMessage、ToolResponseMessage...），
需要统一的序列化/反序列化方案：

```
正向：Message → JSON（OpenAI 格式）
  AssistantMessage  → {"role":"assistant","content":"...","tool_calls":[...]}
  UserMessage       → {"role":"user","content":"..."}
  ToolResponseMsg   → 多条 {"role":"tool","tool_call_id":"...","name":"...","content":"..."}

反向：JSON → Message
  连续多条 role=tool → 合并为单个 ToolResponseMessage
```

## 使用示例

```java
// 配置数据源（Spring Boot 自动注入）
DataSource dataSource = ...;

ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .instructions("你是一个有帮助的助手")
    .dataSource(dataSource)          // 新增！
    .enableSession(true)             // 新增！开启会话持久化
    .build();

// 第一次调用
agent.call("我叫小明", RunnableParams.builder()
    .conversationId("conv-001")
    .build());

// 第二次调用 —— Agent 记得你叫小明！
String answer = agent.call("我叫什么名字？", RunnableParams.builder()
    .conversationId("conv-001")      // 同一个 conversationId
    .build());
// → "你叫小明。"
```

## 与上节对比

| 维度 | 第 05 节 | 第 06 节 |
|------|---------|---------|
| 记忆 | 无状态，每次从零开始 | 通过 DB 持久化实现多轮记忆 |
| ReactAgent 属性 | chatModel, instructions, maxRounds, tools | + dataSource, enableSession |
| AgentLoopExecutor | 纯内存循环 | + 开局加载历史 / 终态批量落库 |
| 依赖 | reactor-core | + spring-jdbc, mybatis-plus |

## ReactAgent 属性增长

```
第03节: chatModel, instructions
第04节: + maxRounds, tools
第05节: + stream() 方法
第06节: + dataSource, enableSession  ← 本节
```

## 本节小结

✅ 理解会话持久化的「两张表」设计
✅ 掌握 MessageJsonSerializer 的双向序列化
✅ 了解「终态批量落库」策略（ReAct 循环中不碰 DB）
✅ 能够通过 conversationId 实现多轮对话记忆
