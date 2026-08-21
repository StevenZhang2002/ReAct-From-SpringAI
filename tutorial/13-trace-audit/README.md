# 第 13 节：追踪审计

> 📚 **这是 Spring AI AgentX 渐进式复现教程的第 13 节**  
> 本教程共 15 节，通过独立 git 分支逐步叠加 feature，最终 1:1 复现完整的 Java Agent 框架。  
> 👉 [查看教程目录](../..) | [← 上一节](../12-long-term-memory/README.md) | [下一节 →](../14-advanced-tools/README.md)

## 本节目标

实现追踪审计系统：记录每次 LLM 调用的详细信息（输入、输出、token 数、耗时等），用于调试、监控和成本分析。

## 上节回顾

第 12 节实现了长期记忆，让 Agent 能够跨会话记住关键信息。但缺乏对 Agent 执行过程的追踪和审计能力。

## 本节新增

- ✅ `TraceStore` — Trace 审计存储层
- ✅ `TraceManager` — 追踪管理器
- ✅ `ReactAgent` 新增 `enableTrace` 属性

## 核心设计

### Trace 数据模型

```java
public record TraceRecord(
    long id,
    long sessionId,
    String conversationId,
    int round,              // ReAct 循环轮次
    String inputData,       // 输入内容（消息序列 JSON）
    String outputData,      // 输出内容（模型回答）
    String think,           // 思考内容
    int promptTokens,       // 提示 token 数
    int completionTokens,   // 补全 token 数
    long durationMs,        // 本轮耗时（毫秒）
    boolean success,        // 是否成功
    String errorMessage     // 错误信息
) {}
```

### 数据库表结构

```sql
CREATE TABLE agentx_trace (
    id                BIGINT       NOT NULL,
    session_id        BIGINT       NOT NULL,
    conversation_id   VARCHAR(100) NOT NULL,
    round             INT          NOT NULL,
    input_data        LONGTEXT     DEFAULT NULL,
    output_data       LONGTEXT     DEFAULT NULL,
    think             LONGTEXT     DEFAULT NULL,
    prompt_tokens     INT          DEFAULT 0,
    completion_tokens INT          DEFAULT 0,
    duration_ms       BIGINT       DEFAULT 0,
    success           INT          DEFAULT 1,
    error_message     LONGTEXT     DEFAULT NULL,
    created_at        TIMESTAMP    DEFAULT NULL,
    PRIMARY KEY (id)
);
```

### 使用示例

```java
ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .dataSource(dataSource)
    .enableSession(true)
    .enableTrace(true)  // 启用追踪审计
    .build();

// 执行后，trace 记录自动写入 agentx_trace 表
agent.call("你好");

// 可查询历史 trace 进行分析
// SELECT * FROM agentx_trace WHERE conversation_id = 'xxx' ORDER BY round;
```

### 追踪流程

```
AgentLoopExecutor.scheduleRound()
  │
  ├── 记录开始时间
  │
  ├── LLM 调用
  │
  ├── 记录结束时间
  │
  └── traceManager.trace(round, input, output, think, tokens, duration)
```

## 与上节对比

| 维度 | 第 12 节 | 第 13 节 |
|------|---------|---------|
| 可观测性 | 无 | + 追踪审计 |
| 调试能力 | 日志 | + 详细 trace 记录 |
| ReactAgent 属性 | + longTermMemory | + enableTrace |

## ReactAgent 属性增长

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
第13节: + enableTrace  ← 本节
```

## 本节小结

✅ 理解追踪审计的设计模式
✅ 掌握 trace 数据模型
✅ 实现 trace 存储层
✅ 能够在 Agent 执行过程中自动记录 trace
