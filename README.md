# Spring AI AgentX 渐进式复现教程

受 [Spring AI AgentX](https://github.com/StevenZhang2002/ReAct-From-SpringAI) 项目启发，通过 **15 节渐进式教程**，从零开始 1:1 复现一个完整的 Java Agent 框架。

> JDK 21+ | Spring Boot 3.5.x | Spring AI 1.1.0

## 项目简介

Spring AI AgentX 是一个基于原生 Spring AI 的 ReAct Agent 框架，提供了 ReAct 执行引擎、会话记忆、长期记忆、上下文压缩、Human-in-the-Loop、Hook 机制等丰富的 Agent 能力。

本项目将 AgentX 的核心功能拆解为 15 个循序渐进的小节，每节以独立 git 分支呈现，逐步叠加 feature，最终完整复现原项目。每节均配有教学文档（README.md）和可编译代码。

## 教程目录

| 节 | 分支 | 主题 | 核心内容 |
|---|------|------|----------|
| 01 | `tutorial/01-project-skeleton` | 项目骨架 | Maven 多模块结构、pom.xml 配置 |
| 02 | `tutorial/02-core-models` | 核心模型 | ReactAgent、AgentResult、RunnableParams 等基础类型 |
| 03 | `tutorial/03-minimal-agent` | 最小可用 Agent | 同步调用、ReAct 循环基础 |
| 04 | `tutorial/04-react-loop` | ReAct 循环 | 工具调用、消息构建、多轮推理 |
| 05 | `tutorial/05-streaming` | 流式输出 | Flux/Sink 流式模型、stream() 方法 |
| 06 | `tutorial/06-session-persistence` | 会话持久化 | SessionMessageStore、SessionPersister、JDBC 存储 |
| 07 | `tutorial/07-params-concurrency` | 动态参数与并发 | RunnableParams 工具参数注入、AgentTaskManager 并发控制 |
| 08 | `tutorial/08-thinking-mode` | 思考模型适配 | ThinkingMode、ThinkTagParser、DeepSeek 兼容 |
| 09 | `tutorial/09-hitl` | Human-in-the-Loop | PauseState、AskUserTool、中断与恢复 |
| 10 | `tutorial/10-hook-system` | Hook 机制 | AgentHook、HookManager、生命周期事件 |
| 11 | `tutorial/11-context-compression` | 上下文压缩 | ContextPolicy、TokenEstimator、ContextCompactor |
| 12 | `tutorial/12-long-term-memory` | 长期记忆 | LongTermMemoryManager、VectorStore、跨会话记忆 |
| 13 | `tutorial/13-trace-audit` | 追踪审计 | TraceStore、TraceManager、LLM 调用记录 |
| 14 | `tutorial/14-advanced-tools` | 高级工具 | TodoWriteTool、任务追踪 |
| 15 | `tutorial/15-advanced-features` | 高级特性 | SubAgentTool 子代理、ToolSearchTool 工具搜索 |

## 如何使用

### 方式一：按顺序逐节学习

```bash
# 克隆项目
git clone https://github.com/StevenZhang2002/ReAct-From-SpringAI.git
cd spring-ai-agentx

# 从第 1 节开始
git checkout tutorial/01-project-skeleton
# 阅读 tutorial/01-project-skeleton/README.md

# 进入第 2 节
git checkout tutorial/02-core-models
# 阅读 tutorial/02-core-models/README.md

# ... 依次类推
```

每节的 `README.md` 包含：
- **本节目标** — 要实现的 feature
- **上节回顾** — 前一节完成的内容
- **本节新增** — 新增/修改的文件清单
- **核心设计** — 关键代码讲解
- **与上节对比** — 差异表格

### 方式二：直接查看最终结果

```bash
git checkout tutorial/15-advanced-features
```

该分支包含全部 15 节的累积代码，即完整复现的 Agent 框架。

## ReactAgent 渐进式增长

教程的核心设计理念是**渐进式属性增长**——ReactAgent 不是一次性带上所有属性，而是随着教程推进逐步叠加：

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

这样学习者可以清晰理解每个属性的引入时机和设计动机。

## 技术栈

- **Spring AI 1.1.0** — ChatClient / ChatModel / ToolCallback / ToolCallingChatOptions
- **Reactor Core** — Flux / Sink 流式输出
- **Java 21** — sealed 接口、record 模式匹配
- **Spring JDBC** — JdbcTemplate 会话持久化
- **Maven** — 多模块构建

## 原项目

本教程复现的原项目：[Spring AI AgentX](https://github.com/StevenZhang2002/ReAct-From-SpringAI)

原项目能力总览：
- ReAct Agent 引擎（Reasoning + Acting 多轮执行闭环）
- 同步与流式双模式输出
- 当前会话记忆（三态模型）+ 长期记忆（VectorStore）
- 6 层渐进式上下文压缩
- Human-in-the-Loop（审批/输入类工具）
- Hook 生命周期机制（7 个事件）
- SubAgent 子代理委派
- ToolSearch 动态工具发现
- TraceAudit 追踪审计
- 沙箱隔离执行（Docker / Local）

## License

This project is licensed under the [Apache License 2.0](LICENSE).
