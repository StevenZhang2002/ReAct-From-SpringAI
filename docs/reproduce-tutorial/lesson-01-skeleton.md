# Lesson 01：Maven 骨架与基础模型层

> 分支：`lesson-01-skeleton` ｜ 上一节：无（起点）

## 本节目标

学完本节后，读者能：

- 搭建 Spring AI AgentX 的 Maven 多模块项目骨架
- 理解框架所有核心数据模型的设计思路
- 掌握 `AgentResult`、`AgentStreamEvent`、`RunnableParams` 三大核心模型
- 理解异常体系 `AgentException` + `AgentErrorCode` 的统一设计
- 了解暂停状态 `PauseState` 和中断相关枚举的设计

## 与上一节对比

这是教程起点，没有"上一节"。本节从零建立：

| 新增内容 | 文件路径 | 说明 |
|----------|----------|------|
| 父 POM | `pom.xml` | Maven 多模块根，管理全局依赖版本 |
| Core 模块 POM | `spring-ai-agentx-core/pom.xml` | 核心模块依赖声明 |
| 执行结果模型 | `core/model/AgentResult.java` | sealed 接口，三种终态 |
| 流式事件模型 | `core/model/AgentStreamEvent.java` | sealed 接口，7 种事件类型 |
| 调用参数 | `core/model/RunnableParams.java` | Builder 模式，会话/工具/输出参数 |
| 结构化输出类型 | `core/model/OutputType.java` | 泛型包装，支持 JSON schema |
| 思考模式枚举 | `core/model/ThinkingMode.java` | 三种思考模型适配 |
| 暂停状态快照 | `core/model/PauseState.java` | 恢复所需完整快照 |
| 暂停工具调用 | `core/model/PendingToolCall.java` | record，暂停的工具信息 |
| 工具执行记录 | `core/model/ToolRecord.java` | record，供 Hook 使用 |
| 子代理来源标识 | `core/model/SubAgentSource.java` | record，区分事件来源 |
| 统一异常 | `core/exception/AgentException.java` | 带错误码的运行时异常 |
| 错误码枚举 | `core/exception/AgentErrorCode.java` | 分类错误码 |
| 暂停原因枚举 | `core/interrupt/PauseReason.java` | HITL vs 用户中断 |
| 安全点枚举 | `core/interrupt/SafePoint.java` | 中断阶段标记 |

## 核心概念

### 1. Maven 多模块结构

项目采用 Maven 多模块结构：

```
spring-ai-agentx/              ← 父 POM（版本管理）
├── spring-ai-agentx-core/     ← 核心框架（本教程重点）
└── spring-ai-agentx-samples/  ← 示例代码
```

**父 POM 的关键决策**：
- 基于 `spring-boot-starter-parent:3.5.6`
- 通过 `spring-ai-bom:1.1.0` 统一管理 Spring AI 依赖版本
- JDK 21 编译目标，开启 `-parameters` 参数（运行时参数名保留）
- `lombok` 设为 `optional`，不强制下游使用

**Core 模块的关键依赖**：

| 依赖 | 用途 | 是否必须 |
|------|------|----------|
| `spring-ai-starter-model-openai` | OpenAI 兼容模型接入 | 是 |
| `spring-ai-starter-model-deepseek` | DeepSeek 模型适配 | 是 |
| `reactor-core` | 响应式流（Flux 驱动多轮循环） | 是 |
| `spring-jdbc` | 会话持久化 | 可选 |
| `mybatis-plus-spring-boot3-starter` | ORM 层 | 是 |
| `spring-ai-vector-store` | 长期记忆 RAG | 可选 |
| `graalvm-polyglot` | Python 脚本执行 | 可选 |
| `fastjson2` | JSON 序列化/修复 | 是 |
| `jieba-analysis` | ToolSearch 中文分词 | 是 |

### 2. AgentResult — 执行终态的 sealed 接口

```java
// core/model/AgentResult.java
public sealed interface AgentResult 
    permits Completed, Paused, Failed {

    record Completed(String answer, String think) implements AgentResult { }
    record Paused(PauseState state) implements AgentResult { }
    record Failed(String error, AgentErrorCode code) implements AgentResult { }
}
```

**设计要点**：
- **sealed 接口**：Java 17+ 特性，穷举所有可能的结果类型，编译器帮助检查完整性
- **模式匹配友好**：`if (result instanceof Completed c)` 直接解构
- **Completed** 包含 `answer`（最终回答）和 `think`（思考内容，可为 null）
- **Paused** 携带 `PauseState`，包含恢复所需的全部信息
- **Failed** 携带 `AgentErrorCode`，调用方可按错误码做差异化处理

### 3. AgentStreamEvent — 流式事件的 sealed 接口

```java
// core/model/AgentStreamEvent.java
public sealed interface AgentStreamEvent permits
    Thinking, Text, ToolStart, ToolEnd, Paused, Error, Complete {

    record Thinking(String content, SubAgentSource source) implements AgentStreamEvent { }
    record Text(String content, SubAgentSource source) implements AgentStreamEvent { }
    record ToolStart(String toolName, String toolCallId, String arguments, SubAgentSource source) { }
    record ToolEnd(String toolName, String toolCallId, String result, SubAgentSource source) { }
    record Paused(PauseState state, SubAgentSource source) implements AgentStreamEvent { }
    record Error(AgentErrorCode code, String message, String detail, SubAgentSource source) { }
    record Complete(long totalPromptTokens, long totalCompletionTokens, ...) { }
}
```

**设计要点**：
- 7 种事件覆盖 Agent 执行的完整生命周期
- 每个事件都携带 `SubAgentSource source`（null 表示主 Agent），支持 SubAgent 事件区分
- Jackson 多态序列化：`@JsonTypeInfo` + `@JsonSubTypes`，自动携带 `"type"` 字段
- 这是 Reactor `Flux<AgentStreamEvent>` 的元素类型

### 4. RunnableParams — 调用参数容器

```java
// core/model/RunnableParams.java
public class RunnableParams {
    private final String conversationId;  // 会话 ID（会话管理 + 并发控制）
    private final String userId;          // 用户 ID（长期记忆维度）
    private final Map<String, Object> customParams;  // 注入系统提示词（LLM 可见）
    private final Map<String, Object> toolParams;    // 工具参数注入（LLM 不可见）
    private final OutputType outputType;  // 结构化输出类型
}
```

**设计要点**：
- `conversationId` 和 `userId` 是两个核心维度：前者管会话状态，后者管长期记忆
- `customParams` vs `toolParams`：前者注入 system prompt（LLM 能看到），后者只在工具执行前注入（安全参数如 token/userId 不暴露给 LLM）
- `OutputType` 支持 per-call 结构化输出，不影响同一会话中的普通对话
- Builder 模式 + Jackson `@JsonCreator` 双构造路径（运行时构造 + 序列化反序列化）

### 5. PauseState — 暂停状态完整快照

```java
// core/model/PauseState.java
public class PauseState {
    private final List<Message> messages;         // 当前消息链
    private final int currentRound;               // 当前轮次
    private final List<PendingToolCall> pendingToolCalls;  // 待处理工具调用
    private final RunnableParams params;          // 原始调用参数
    private final String query;                   // 原始用户输入
    private final PauseReason reason;             // 暂停原因
    private final SafePoint safePoint;            // 中断安全点
    // ... token 统计、时间戳、子代理状态等
}
```

**设计要点**：
- 包含恢复执行的**全部**信息，可序列化持久化到外部存储
- `PauseReason` 区分两种暂停语义：HITL 审批 vs 用户中断
- `SafePoint` 标记中断时所在阶段，决定恢复策略
- `children` 字段支持 SubAgent 中断状态的嵌套保存

### 6. 异常体系 — 统一错误处理

```java
// core/exception/AgentErrorCode.java
public enum AgentErrorCode {
    LLM_CALL_FAILED("E1001"),       // LLM 调用失败（重试耗尽）
    LLM_EMPTY_RESPONSE("E1002"),    // LLM 返回空响应
    CONCURRENT_EXECUTION("E2001"),  // 会话并发执行
    SANDBOX_IMAGE_PULL_FAILED("E3001"),  // 沙箱镜像拉取失败
    SANDBOX_EXEC_TIMEOUT("E3002"),       // 沙箱命令超时
    SANDBOX_CONTAINER_NOT_FOUND("E3003"); // 沙箱容器不存在
}

// core/exception/AgentException.java
public class AgentException extends RuntimeException {
    private final AgentErrorCode code;
}
```

**设计要点**：
- 所有框架异常统一为 `AgentException`，携带 `AgentErrorCode`
- 错误码按类别分段：E1xxx = LLM 相关，E2xxx = 并发控制，E3xxx = 沙箱
- 调用方可通过 `getCode()` 做差异化处理

## 代码走读

按阅读顺序：

1. **`pom.xml`**（父 POM）：版本管理中心。注意 `spring-ai-bom` 的 import scope 和 `java.version=21`
2. **`spring-ai-agentx-core/pom.xml`**：核心模块依赖。注意哪些是 optional（如 vector-store、graalvm），这些对应框架的可选功能
3. **`core/model/AgentResult.java`**：从终态开始理解——Agent 执行只有三种结局。sealed 接口是 Java 17+ 的代数数据类型
4. **`core/model/AgentStreamEvent.java`**：流式场景下，每一步产出什么事件。7 种事件对应 Agent 生命周期的 7 个阶段
5. **`core/model/RunnableParams.java`**：调用方传入的参数。理解 `customParams` vs `toolParams` 的安全边界
6. **`core/model/OutputType.java`**：结构化输出的类型包装。注意 `listOf()` 用匿名 `ParameterizedType` 表示泛型
7. **`core/model/PauseState.java`**：暂停快照。这是 HITL 和中断恢复的数据基础
8. **`core/model/PendingToolCall.java`**：简单的 record，记录被暂停的工具调用
9. **`core/model/ThinkingMode.java`**：三种模式——DISABLED / THINK_TAG / REASONING_CONTENT
10. **`core/model/SubAgentSource.java`**：SubAgent 事件来源标识
11. **`core/model/ToolRecord.java`**：工具执行记录
12. **`core/exception/AgentException.java`** + **`AgentErrorCode.java`**：统一异常体系
13. **`core/interrupt/PauseReason.java`** + **`SafePoint.java`**：中断相关枚举

## 关键数据流

本节只建立数据模型，尚无执行流程。但模型之间的关系已经勾勒出整体架构轮廓：

```
用户调用
  │
  ▼
RunnableParams ───────────────┐
  │ conversationId             │
  │ userId                     │
  │ customParams / toolParams  │
  │ outputType                 │
  ▼                            │
Agent 执行引擎（Lesson 03+）    │
  │                            │
  ├── 成功 → AgentResult.Completed(answer, think)
  ├── 暂停 → AgentResult.Paused(PauseState)
  │              ├── reason: HITL / USER_INTERRUPT
  │              ├── safePoint: INIT / LLM / TOOL
  │              ├── pendingToolCalls: [PendingToolCall]
  │              └── messages: [Message]
  └── 失败 → AgentResult.Failed(error, AgentErrorCode)

流式场景：
  Flux<AgentStreamEvent>
    ├── Thinking(content)
    ├── Text(content)
    ├── ToolStart(name, id, args)
    ├── ToolEnd(name, id, result)
    ├── Paused(PauseState)
    ├── Error(code, message, detail)
    └── Complete(tokens, conversationId, sessionId)
```

## 设计决策与思考

### 为什么用 sealed 接口而不是 enum？

`AgentResult` 和 `AgentStreamEvent` 都用了 sealed interface + record 的组合，而不是传统 enum。原因：

1. **record 可以携带任意数据**：enum 的每个常量是单例，无法携带每次不同的数据
2. **sealed 保证穷举**：编译器知道所有可能的类型，模式匹配时不遗漏
3. **可扩展性**：新增事件类型只需添加一个 record + `@JsonSubTypes.Type`

### 为什么 customParams 和 toolParams 要分开？

安全边界考虑。`customParams` 注入到 system prompt，LLM 能看到并可能在回答中泄露。`toolParams` 只在工具执行时注入，LLM 不知道这些值的存在。像 `userId`、`authToken` 这类敏感信息应该放在 `toolParams` 中。

### 为什么 PauseState 要包含完整 messages？

恢复时需要从暂停点继续，而 LLM 的上下文是完整的消息链。如果只存增量，恢复时需要重新组装，容易出错。存完整快照虽然占用更多空间，但恢复逻辑简单可靠。

## 动手实践

- [ ] 创建 Maven 多模块项目，配置父 POM 和 core 模块 POM
- [ ] 尝试编译 `AgentResult` 的 sealed 接口，体验模式匹配
- [ ] 思考：如果要新增一种 `AgentResult` 类型（如 `Cancelled`），需要修改哪些地方？
- [ ] 用 Jackson 序列化一个 `AgentStreamEvent.Text` 对象，观察输出的 JSON 结构

## 小结与预告

- **核心收获**：
  1. 掌握了框架的 Maven 多模块结构和关键依赖
  2. 理解了 Agent 的三种终态（Completed / Paused / Failed）和七种流式事件
  3. 理解了调用参数的安全边界（customParams vs toolParams）
  4. 了解了暂停状态快照的设计，为后续 HITL 和中断恢复打基础
- **下一节将学习**：构建 `ReactAgent` 的 Builder 模式和 `ChatClient` 的组装方式——让 Agent 真正"跑起来"的第一步
