# 第 11 节：上下文压缩

## 本节目标

实现上下文压缩系统：当对话历史过长时，自动压缩旧消息，避免超出 LLM 上下文窗口限制。

## 上节回顾

第 10 节实现了 Hook 系统，允许用户在 Agent 执行的关键节点插入自定义逻辑。但长对话仍可能超出 LLM 上下文窗口限制，导致调用失败。

## 本节新增

- ✅ `ContextPolicy` — 压缩策略配置（阈值、保护区大小）
- ✅ `TokenEstimator` — Token 估算工具（区分中英文）
- ✅ `ContextCompactor` — 压缩器主入口
- ✅ `ReactAgent` 新增 `contextPolicy` 属性

## 核心设计

### 压缩触发条件

```java
// 外层门禁：消息数或 token 超限才触发压缩
if (messages.size() >= policy.msgThreshold()) {
    // 触发压缩
}
if (estimatedTokens >= policy.tokenThreshold()) {
    // 触发压缩
}
```

### Token 估算

区分中英文进行差异化估算：
- 英文/ASCII 字符：约 4 字符 = 1 token
- 中文/CJK 字符：约 1.5 字符 = 1 token

```java
public static int estimateTokens(String text) {
    int cjkCount = 0;      // 中文字符数
    int nonCjkCount = 0;   // 英文字符数
    for (char ch : text.toCharArray()) {
        if (isCJK(ch)) cjkCount++;
        else nonCjkCount++;
    }
    return (int) (cjkCount / 1.5 + nonCjkCount / 4.0);
}
```

### 压缩策略配置

```java
public record ContextPolicy(
    int lastKeep,              // 保护区大小（最近 N 条消息不压缩）
    int msgThreshold,          // 消息数阈值
    int tokenThreshold,        // token 阈值
    int largePayloadTokens,    // 大消息 token 阈值
    // ... 其他配置
) {
    public static ContextPolicy defaults() {
        return new ContextPolicy(
            50,      // lastKeep: 保护最近 50 条消息
            100,     // msgThreshold: 100 条消息触发
            90000,   // tokenThreshold: 90K token 触发
            // ...
        );
    }
}
```

### 压缩器工作流程

```
AgentLoopExecutor.scheduleRound()
  │
  ├── 构建 messages
  │
  ├── contextCompactor.compact(messages)
  │   ├── 检查外层门禁（消息数/token）
  │   ├── 遍历压缩策略链
  │   └── 执行压缩（截断/摘要/移除）
  │
  └── LLM 调用
```

## 使用示例

```java
ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .contextPolicy(ContextPolicy.builder()
        .lastKeep(30)           // 保护最近 30 条消息
        .tokenThreshold(50000)  // 50K token 触发压缩
        .build())
    .build();

// 长对话时自动触发压缩
String answer = agent.call("问题1");
// ... 多轮对话后
answer = agent.call("问题N");  // 自动压缩旧消息
```

## 与上节对比

| 维度 | 第 10 节 | 第 11 节 |
|------|---------|---------|
| 扩展性 | Hook 系统 | + 上下文压缩 |
| 长对话支持 | 无 | 自动压缩 |
| ReactAgent 属性 | + hooks | + contextPolicy |

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
第11节: + contextPolicy  ← 本节
```

## 本节小结

✅ 理解上下文压缩的必要性
✅ 掌握 Token 估算方法（区分中英文）
✅ 实现压缩策略配置
✅ 能够通过配置控制压缩行为
