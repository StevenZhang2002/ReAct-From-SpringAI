# 第 12 节：长期记忆

> 📚 **这是 Spring AI AgentX 渐进式复现教程的第 12 节**  
> 本教程共 15 节，通过独立 git 分支逐步叠加 feature，最终 1:1 复现完整的 Java Agent 框架。  
> 👉 [查看教程目录](../..) | [← 上一节](../11-context-compression/README.md) | [下一节 →](../13-trace-audit/README.md)

## 本节目标

实现跨会话的长期记忆系统：Agent 能够从对话中提取关键信息，存储到向量数据库，并在后续对话中检索相关记忆注入上下文。

## 上节回顾

第 11 节实现了上下文压缩，解决长对话超出 LLM 上下文窗口的问题。但会话结束后，对话内容就丢失了。长期记忆让 Agent 能够"记住"跨会话的关键信息。

## 本节新增

- ✅ `LongTermMemoryConfig` — 长期记忆配置（VectorStore、检索参数）
- ✅ `LongTermMemoryManager` — 记忆管理器（抽取、去重、合并、检索）
- ✅ `ReactAgent` 新增 `longTermMemory` 属性

## 核心设计

### 记忆流程

```
对话结束
  │
  ├── 1. 抽取候选记忆（LLM 调用）
  │
  ├── 2. 对每条候选：向量检索相似记忆
  │   ├── 命中 → LLM 合并旧 + 新 → 替换
  │   └── 未命中 → 直接插入
  │
  └── 3. 下次对话：按 query 检索相关记忆注入 SystemMessage
```

### 配置参数

```java
public class LongTermMemoryConfig {
    private final VectorStore vectorStore;      // 向量存储
    private final int topK;                     // 检索数量
    private final double similarityThreshold;   // 相似度阈值
    private final int dedupTopK;                // 去重检索数量
    private final double dedupThreshold;        // 去重阈值
    private final String extractPrompt;         // 抽取提示词
    private final String mergePrompt;           // 合并提示词
}
```

### 使用示例

```java
// 配置向量存储（如 PgVector）
VectorStore vectorStore = new PgVectorStore(dataSource);

LongTermMemoryConfig memoryConfig = LongTermMemoryConfig.builder()
    .vectorStore(vectorStore)
    .topK(5)                    // 检索 5 条相关记忆
    .similarityThreshold(0.5)   // 相似度阈值
    .build();

ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .longTermMemory(memoryConfig)
    .build();

// 第一次对话：提取并存储记忆
agent.call("我叫张三，我是一名 Java 开发者");

// 第二次对话：自动检索相关记忆
agent.call("你还记得我的名字吗？");  // Agent 会记得用户叫张三
```

### 记忆抽取

```java
// 从对话记录抽取候选记忆
List<String> candidates = extractCandidates(transcript);
// 返回类似：["用户名叫张三", "用户是 Java 开发者"]
```

### 记忆检索注入

```java
// 按 query 检索相关记忆
List<Document> memories = memoryManager.searchRelevant(userId, query);

// 注入到 SystemMessage
String memoryContext = formatMemories(memories);
// "[相关记忆]\n- 用户名叫张三\n- 用户是 Java 开发者"
```

## 与上节对比

| 维度 | 第 11 节 | 第 12 节 |
|------|---------|---------|
| 记忆类型 | 仅会话内 | + 跨会话长期记忆 |
| 存储 | 无 | 向量数据库 |
| ReactAgent 属性 | + contextPolicy | + longTermMemory |

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
第12节: + longTermMemory  ← 本节
```

## 本节小结

✅ 理解长期记忆的设计模式
✅ 掌握向量存储的使用
✅ 实现记忆的抽取、去重、合并、检索流程
✅ 能够将检索到的记忆注入上下文
