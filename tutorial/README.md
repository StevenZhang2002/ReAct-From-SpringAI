# Spring AI AgentX 渐进式复现教程

## 教程总览

从零开始，通过 15 节渐进式教程，1:1 复现 `spring-ai-agentx-core` 项目。
每节以独立 git 分支呈现，基于上一节逐步叠加 feature。

## 教程路线

| # | 分支 | 核心 Feature | 关键类 |
|---|------|-------------|-------|
| 01 | `tutorial/01-project-skeleton` | Maven 多模块骨架 | pom.xml |
| 02 | `tutorial/02-core-models` | 核心数据模型 | AgentResult, AgentStreamEvent, RunnableParams |
| 03 | `tutorial/03-minimal-agent` | 最小可用 Agent | ReactAgent(2属性), AgentLoopExecutor(单轮) |
| 04 | `tutorial/04-react-loop` | ReAct 多轮循环 | ToolCallExecutor, maxRounds, 工具检测 |
| 05 | `tutorial/05-streaming` | Flux 流式输出 | Sink, scheduleRound, processChunk |
| 06 | `tutorial/06-session-persistence` | 会话持久化 | SessionMessageStore, ConversationStore |
| 07 | `tutorial/07-params-concurrency` | 动态参数 + 并发控制 | toolParams替换, AgentTaskManager |
| 08 | `tutorial/08-thinking-mode` | 思考模型适配 | ThinkingModeProcessor, ThinkTagParser |
| 09 | `tutorial/09-hitl` | Human-in-the-Loop | PauseAdvisor, AskUserTool, PauseState |
| 10 | `tutorial/10-hook-system` | Hook 生命周期 | AgentHook, HookManager, 事件体系 |
| 11 | `tutorial/11-context-compression` | 上下文压缩 | ContextPolicy, 6层压缩策略 |
| 12 | `tutorial/12-long-term-memory` | 长期记忆 | LongTermMemoryManager, VectorStore |
| 13 | `tutorial/13-trace-structured` | 追踪审计 + 结构化输出 | TraceManager, RequestLoggingAdvisor |
| 14 | `tutorial/14-advanced-tools` | 高级内置工具 | TodoWrite, Bash, Python, FileSystem |
| 15 | `tutorial/15-advanced-features` | SubAgent + ToolSearch + Sandbox | DeferredToolRegistry, SubAgentTool |

## ReactAgent 渐进式属性增长

```
第03节: chatModel, instructions
第04节: + maxRounds, tools
第05节: + stream() 方法
第06节: + dataSource, sessionMessageStore, conversationStore, enableSession
第07节: + taskManager (并发控制)
第08节: + thinkingMode
第09节: + askUser, hooks(PauseAdvisor), stateStore
第10节: + hooks (用户自定义)
第11节: + contextPolicy
第12节: + longTermMemoryConfig, longTermMemoryManager
第13节: + enableTrace, traceStore
第14节: (工具层变化，ReactAgent 不变)
第15节: + deferredToolRegistry, sandboxConfig, subAgentProviders
```

## 如何使用

```bash
# 查看第 01 节
git checkout tutorial/01-project-skeleton
cat tutorial/01-project-skeleton/README.md

# 查看第 05 节（流式输出）
git checkout tutorial/05-streaming
cat tutorial/05-streaming/README.md

# 对比两节之间的差异
git diff tutorial/04-react-loop..tutorial/05-streaming -- tutorial/
```

## 技术栈

- Java 21
- Spring Boot 3.5.6
- Spring AI 1.1.0
- Reactor Core (流式)
- FastJSON2 (JSON 处理)
- MyBatis-Plus (ORM)
- PgVector (向量存储，长期记忆)
- GraalVM Polyglot (Python 执行)

## 前置知识

1. Spring Boot 基础
2. Spring AI ChatClient/ChatModel
3. Reactor (Flux/Sink) 基础
4. Java sealed 接口和 record
