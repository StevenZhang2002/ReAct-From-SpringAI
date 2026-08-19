# Spring AI AgentX Core 渐进式复现教程

> **目标**：从零开始，通过 15 个渐进式阶段，一比一复现 `spring-ai-agentx-core` 完整项目。
>
> 每一小节在上一节的基础上**增量添加代码**，附带教学文档、新增内容对比、以及独立 git 分支。

## 设计理念

本教程不是"阅读源码"，而是"动手复现"。每一节你会：

1. **了解本节目标**：本节要实现的 feature 和解决的问题
2. **对比上一节**：明确新增/修改了哪些文件，新增了什么能力
3. **阅读教学文档**：理解设计思路和关键决策
4. **对照代码实现**：每个文件的核心代码都有讲解
5. **切换到对应分支**：`git checkout lesson-XX-slug` 查看完整代码

## 技术栈

- **JDK** 21+
- **Spring Boot** 3.5.x
- **Spring AI** 1.1.0
- **Reactor** (Project Reactor / Flux)
- **MyBatis-Plus** 3.5.12
- **Fastjson2** 2.0.60
- **Lombok** 1.18.30

## 课程总览

| # | 主题 | 新增核心文件 | 分支 | 状态 |
|---|------|-------------|------|------|
| 01 | Maven 骨架与基础模型层 | `pom.xml`, `model/`, `exception/` | `lesson-01-skeleton` | 🔄 进行中 |
| 02 | ReactAgent Builder 与 ChatClient 构建 | `ReactAgent.java` (Builder), `prompt/` | `lesson-02-builder` | ⬜ 未开始 |
| 03 | ReAct 循环引擎核心 | `agent/internal/AgentLoopExecutor.java`, `RoundState`, `RoundMode` | `lesson-03-react-engine` | ⬜ 未开始 |
| 04 | 同步与流式输出 | `AgentStreamEvent.java`, `AgentLoopExecutor` 流式部分 | `lesson-04-sync-stream` | ⬜ 未开始 |
| 05 | 工具调度引擎 | `ToolCallExecutor.java`, `LoopMessageBuilder.java`, `BuiltMessages.java` | `lesson-05-tool-executor` | ⬜ 未开始 |
| 06 | 会话存储与持久化 | `memory/store/`, `SessionPersister.java`, `advisors/` | `lesson-06-session-store` | ⬜ 未开始 |
| 07 | 长期记忆体系 | `LongTermMemoryManager.java`, `memory/util/`, `LongTermMemoryConfig.java` | `lesson-07-long-memory` | ⬜ 未开始 |
| 08 | 上下文压缩框架 | `context/`, `compress/strategy/` 6 层策略 | `lesson-08-compression` | ⬜ 未开始 |
| 09 | HITL 与 PauseAdvisor | `PauseAdvisor.java`, `AskUserTool.java` | `lesson-09-hitl` | ⬜ 未开始 |
| 10 | 中断恢复与 PauseState | `interrupt/`, `PauseState.java`, `PendingToolCall.java` | `lesson-10-interrupt` | ⬜ 未开始 |
| 11 | SubAgent 子代理 | `SubAgentTool.java`, `SubAgentSource.java` | `lesson-11-subagent` | ⬜ 未开始 |
| 12 | Hook 生命周期机制 | `hook/` 全套事件, `HookManager.java`, `AgentRuntimeContext.java` | `lesson-12-hook` | ⬜ 未开始 |
| 13 | TraceAudit 与结构化输出 | `trace/`, `OutputType.java`, `JsonRepairUtil.java` | `lesson-13-trace` | ⬜ 未开始 |
| 14 | 思考模型适配与异常重试 | `ThinkingMode.java`, `ThinkingModeProcessor.java`, `DeepSeekV4ChatModel.java` | `lesson-14-thinking` | ⬜ 未开始 |
| 15 | 沙箱隔离与 ToolSearch | `sandbox/`, `tools/toolsearch/`, 内置工具集 | `lesson-15-sandbox` | ⬜ 未开始 |

## 依赖拓扑（学习顺序的依据）

```
Lesson 01 基础模型
    ↓
Lesson 02 Builder + ChatClient
    ↓
Lesson 03 ReAct 引擎 ← 核心中的核心
    ↓
Lesson 04 同步/流式输出
    ↓
Lesson 05 工具调度 ← 工具执行是 Agent 的"手脚"
    ↓
Lesson 06 会话持久化 ← 有了记忆，Agent 才有连续性
    ↓
Lesson 07 长期记忆 ← 跨会话知识
    ↓
Lesson 08 上下文压缩 ← 长对话必备
    ↓
Lesson 09 HITL ← 人机交互
    ↓
Lesson 10 中断恢复 ← 暂停/恢复能力
    ↓
Lesson 11 SubAgent ← 多 Agent 协作
    ↓
Lesson 12 Hook 机制 ← 全生命周期扩展
    ↓
Lesson 13 Trace + 结构化输出 ← 可观测性
    ↓
Lesson 14 思考模型 + 重试 ← 模型适配
    ↓
Lesson 15 沙箱 + ToolSearch ← 安全执行 + 按需发现
```

## 快速开始

```bash
# 1. 克隆项目
git clone https://github.com/bigchuidw3/spring-ai-agentx.git
cd spring-ai-agentx

# 2. 从第一课开始
git checkout lesson-01-skeleton

# 3. 按顺序学习，每课切换到对应分支查看完整代码
git checkout lesson-02-builder
git checkout lesson-03-react-engine
# ...
```

## 学习建议

- **前置知识**：Java 基础、Spring Boot 基本概念、了解 LLM API 调用
- **阅读顺序**：严格按编号顺序，每节依赖前一节的概念和代码
- **时间投入**：每节 20-40 分钟
- **实践建议**：先阅读教学文档理解设计思路，再对照分支代码学习
