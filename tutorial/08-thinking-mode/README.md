# 第 08 节：思考模型适配

> 📚 **这是 Spring AI AgentX 渐进式复现教程的第 08 节**  
> 本教程共 15 节，通过独立 git 分支逐步叠加 feature，最终 1:1 复现完整的 Java Agent 框架。  
> 👉 [查看教程目录](../..) | [← 上一节](../07-params-concurrency/README.md) | [下一节 →](../09-hitl/README.md)

## 本节目标

支持思考模型（如 DeepSeek-R1、Qwen3）的两种输出格式：
1. **`<think>` 标签格式** — 思考内容包裹在 `<think>...</think>` 中
2. **`reasoning_content` 字段格式** — 思考内容通过独立字段返回

## 上节回顾

第 07 节实现了 toolParams 安全注入和并发控制。但面对思考模型时，Agent 无法区分
「思考过程」和「正式回答」，也无法将思考内容以 Thinking 事件推送给前端。

## 本节新增

- ✅ `AgentStreamEvent.Thinking` — 思考内容事件
- ✅ `ThinkTagParser` — `<think>` 标签解析器（支持跨 chunk 状态追踪）
- ✅ `ThinkingModeProcessor` — 思考模式处理器（封装三模式分支逻辑）
- ✅ `RoundState` 新增 `reasoningBuffer` / `inThink` 字段
- ✅ `ReactAgent` 新增 `thinkingMode` 属性

## 核心设计

### 三种思考模式

| 模式 | 说明 | 适用模型 |
|------|------|---------|
| `DISABLED` | 自动剥离 `<think>` 标签，不发射 Thinking 事件 | 普通模型 |
| `THINK_TAG` | 解析 `<think>` 标签，发射 Thinking + Text 事件 | MiniMax M2.7 |
| `REASONING_CONTENT` | 从 metadata/反射提取 reasoning_content，发射 Thinking 事件 | DeepSeek-R1, Qwen3 |

### ThinkTagParser 跨 chunk 解析

```
chunk1: "<thin"         → 标签不完整，等待下一个 chunk
chunk2: "k>让我想想"     → 标签开始，进入 think 状态
chunk3: "</think>答案是42" → 标签结束，退出 think 状态

状态通过 inThink 参数跨 chunk 传递
```

### 流式处理流程

```
LLM chunk 到达
  │
  ├── REASONING_CONTENT 模式
  │   ├── content → Text 事件
  │   └── reasoning_content (metadata) → Thinking 事件
  │
  ├── THINK_TAG 模式
  │   └── ThinkTagParser.parse(chunk, inThink)
  │       ├── think 标签内 → Thinking 事件
  │       └── think 标签外 → Text 事件
  │
  └── DISABLED 模式
      └── ThinkTagParser.parse(chunk, inThink)
          ├── think 标签内 → 丢弃
          └── think 标签外 → Text 事件
```

## 使用示例

```java
ReactAgent agent = ReactAgent.builder()
    .chatModel(deepSeekModel)
    .instructions("你是一个有帮助的助手")
    .thinkingMode(ThinkingMode.REASONING_CONTENT)  // 新增！
    .build();

// 流式调用
agent.streamForResult("解释量子计算", RunnableParams.empty())
    .doOnNext(event -> {
        switch (event) {
            case AgentStreamEvent.Thinking t ->
                System.out.println("[思考] " + t.content());
            case AgentStreamEvent.Text t ->
                System.out.print(t.content());
            default -> {}
        }
    })
    .blockLast();
```

## 与上节对比

| 维度 | 第 07 节 | 第 08 节 |
|------|---------|---------|
| 思考支持 | 无 | 三种模式适配 |
| 事件类型 | Text/ToolStart/ToolEnd/Error/Complete | + Thinking |
| ReactAgent 属性 | + taskManager | + thinkingMode |
| 新增类 | — | ThinkTagParser, ThinkingModeProcessor |

## ReactAgent 属性增长

```
第03节: chatModel, instructions
第04节: + maxRounds, tools
第05节: + stream() 方法
第06节: + dataSource, enableSession
第07节: + taskManager
第08节: + thinkingMode  ← 本节
```

## 本节小结

✅ 理解思考模型的两种输出格式
✅ 掌握 ThinkTagParser 的跨 chunk 状态追踪
✅ 实现 ThinkingModeProcessor 的三模式分支
✅ 能够通过 Thinking 事件实时推送思考过程
