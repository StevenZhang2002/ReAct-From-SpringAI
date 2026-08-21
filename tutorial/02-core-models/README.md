# 第 02 节：核心模型定义

> 📚 **这是 Spring AI AgentX 渐进式复现教程的第 02 节**  
> 本教程共 15 节，通过独立 git 分支逐步叠加 feature，最终 1:1 复现完整的 Java Agent 框架。  
> 👉 [查看教程目录](../..) | [← 上一节](../01-project-skeleton/README.md) | [下一节 →](../03-minimal-agent/README.md)

## 本节目标

定义 Agent 框架的基础数据模型，为后续 Agent 实现提供数据结构支撑。

## 本节新增

- ✅ `AgentResult` — Agent 执行结果（sealed 接口，Completed/Failed 两种状态）
- ✅ `AgentStreamEvent` — 流式事件（sealed 接口，Text/ToolStart/ToolEnd/Complete 等）
- ✅ `RunnableParams` — 运行时参数（conversationId、userId、customParams、toolParams）
- ✅ `OutputType` — 结构化输出类型描述
- ✅ `ThinkingMode` — 思考模式枚举（DISABLED/THINK_TAG/REASONING_CONTENT）
- ✅ `AgentErrorCode` — 错误码枚举
- ✅ `AgentException` — 框架统一异常

## 上节回顾

第 01 节完成了 Maven 多模块项目搭建和基础依赖配置。本节在此基础上添加核心数据模型。

## 设计思路

### 为什么用 sealed 接口？

Java 17+ 的 sealed 接口提供 **模式匹配** 能力：

```java
// 传统 if-else
if (result instanceof AgentResult.Completed c) {
    System.out.println(c.answer());
} else if (result instanceof AgentResult.Failed f) {
    System.out.println(f.error());
}

// 模式匹配（更优雅）
switch (result) {
    case AgentResult.Completed c -> System.out.println(c.answer());
    case AgentResult.Failed f -> System.out.println(f.error());
}
```

**优势：**
1. **类型安全**：编译器确保处理所有情况
2. **不可扩展**：只有预定义的子类，防止滥用
3. **模式匹配**：配合 switch 表达式，代码更简洁

### AgentResult 设计

```
AgentResult (sealed interface)
├── Completed — 执行完成，包含最终答案
├── Paused    — 执行暂停，等待外部输入（HITL）
└── Failed    — 执行失败
```

**关键决策：**
- `Completed` 包含 `answer`（最终答案）和 `think`（思考内容，可为 null）
- `Paused` 包含 `PauseState`（恢复所需的全部信息）— 本节暂不实现，第 10 节添加
- `Failed` 包含 `error`（错误信息）和 `code`（错误码）

### AgentStreamEvent 设计

```
AgentStreamEvent (sealed interface)
├── Thinking  — LLM 思考过程
├── Text      — LLM 文本输出
├── ToolStart — 工具即将执行
├── ToolEnd   — 工具执行完成
├── Paused    — 执行暂停（第 10 节）
├── Error     — 调用异常
└── Complete  — 执行完成
```

**设计要点：**
- 每个事件都是 record（不可变数据）
- 支持 Jackson 多态序列化（`@JsonTypeInfo`）
- 预留 `SubAgentSource` 字段（第 15 节），标识事件来源

### RunnableParams 设计

```
RunnableParams
├── conversationId — 会话 ID（会话管理 + 并发控制）
├── userId         — 用户 ID（长期记忆标识）
├── customParams   — 自定义参数（注入系统提示词，LLM 可见）
├── toolParams     — 工具参数（不注入提示词，工具执行时替换）
└── outputType     — 结构化输出类型
```

**两种参数的区别：**
- `customParams`：LLM 可见，注入到 SystemMessage 中
  - 例：`addParam("language", "zh-CN")` → LLM 知道要用中文回答
- `toolParams`：LLM 不可见，工具执行前按 inputSchema 注入
  - 例：`addToolParam("userId", "123")` → 工具收到真实 userId，不依赖 LLM 生成

## 代码实现

### 1. AgentErrorCode

```java
public enum AgentErrorCode {
    LLM_CALL_FAILED,
    CONCURRENT_EXECUTION,
    TOOL_EXECUTION_FAILED,
    INVALID_PARAMS
}
```

### 2. AgentException

```java
public class AgentException extends RuntimeException {
    private final AgentErrorCode code;
    // ...
}
```

### 3. ThinkingMode

```java
public enum ThinkingMode {
    DISABLED,             // 不处理思考内容
    THINK_TAG,            // <think>...</think> 标签格式
    REASONING_CONTENT     // reasoning_content 独立字段
}
```

### 4. AgentResult（核心）

```java
public sealed interface AgentResult 
    permits Completed, Paused, Failed {
    
    record Completed(String answer, String think) implements AgentResult {}
    record Paused(PauseState state) implements AgentResult {}  // 第 10 节
    record Failed(String error, AgentErrorCode code) implements AgentResult {}
}
```

### 5. AgentStreamEvent（核心）

```java
@JsonTypeInfo(use = Id.NAME, property = "type")
public sealed interface AgentStreamEvent 
    permits Thinking, Text, ToolStart, ToolEnd, Paused, Error, Complete {
    
    record Text(String content) implements AgentStreamEvent {}
    record ToolStart(String toolName, String toolCallId, String arguments) 
        implements AgentStreamEvent {}
    record ToolEnd(String toolName, String toolCallId, String result) 
        implements AgentStreamEvent {}
    record Complete(long totalPromptTokens, long totalCompletionTokens) 
        implements AgentStreamEvent {}
    // ...
}
```

### 6. RunnableParams（Builder 模式）

```java
public class RunnableParams {
    private final String conversationId;
    private final String userId;
    private final Map<String, Object> customParams;
    private final Map<String, Object> toolParams;
    private final OutputType outputType;
    
    // Builder 模式构造
    public static Builder builder() { return new Builder(); }
}
```

## 与原始项目的对比

| 特性 | 本节实现 | 原始项目 |
|------|---------|---------|
| AgentResult | Completed + Failed | + Paused（第 10 节） |
| AgentStreamEvent | 4 种事件 | + Paused + SubAgentSource |
| RunnableParams | 基础版本 | 相同 |
| ThinkingMode | 3 种模式 | 相同 |

## 编译验证

```bash
mvn clean compile -pl spring-ai-agentx-core
```

## 本节小结

✅ 定义了 7 个核心模型类
✅ 使用 sealed 接口实现类型安全的结果/事件
✅ 使用 Builder 模式构造复杂参数对象
✅ 区分 customParams（LLM 可见）和 toolParams（工具可见）

## 下节预告

**第 03 节：最小可用 Agent**

将实现：
- `ReactAgent` — Builder 模式，最简版本（chatModel + instructions + call）
- `AgentLoopExecutor` — 单轮 LLM 调用
- `LoopMessageBuilder` — 构建 SystemMessage + UserMessage

目标：实现一个能调用 LLM 并返回文本的最小 Agent。
