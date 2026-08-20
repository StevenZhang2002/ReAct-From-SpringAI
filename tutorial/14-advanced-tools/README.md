# 第 14 节：高级内置工具

## 本节目标

实现高级内置工具：提供任务管理（TodoWrite）和 Shell 命令执行（Bash）等实用工具，增强 Agent 的能力。

## 上节回顾

第 13 节实现了追踪审计，可以记录 Agent 执行过程。但 Agent 缺乏一些实用工具来完成任务。

## 本节新增

- ✅ `TodoWriteTool` — 任务列表管理工具
- ✅ `BashTool` — Shell 命令执行工具

## 核心设计

### TodoWriteTool

灵感来源于 Claude Code 的任务管理工具，让 Agent 能够创建和追踪任务列表。

```java
@Tool(name = "TodoWrite", description = "创建和管理结构化任务列表")
public String todoWrite(List<TodoItem> todos) {
    validateTodos(todos);
    return "任务列表已成功更新";
}

public record TodoItem(
    String content,      // 任务内容（祈使形式）
    Status status,       // pending / in_progress / completed
    String activeForm    // 执行时显示（现在进行时）
) {}
```

**校验规则：**
- 同一时间只能有一个 in_progress 任务
- content 和 activeForm 不能为空
- 状态值必须是 pending、in_progress 或 completed

### BashTool

Shell 命令执行工具，支持持久化会话。

```java
@Tool(name = "bash", description = "在持久化 Shell 会话中执行命令")
public String executeShellCommand(
    String command,      // 要执行的命令
    Boolean restart,     // 是否重启会话
    Long timeoutMs       // 超时时间
) {
    // 执行命令并返回结果
}
```

**特性：**
- 工作目录持久化
- 环境变量持久化
- 支持超时控制
- 动态生成工具描述（根据操作系统）

### 使用示例

```java
// 创建 Agent 并添加工具
ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .tools(TodoWriteTool.create())      // 任务管理
    .tools(BashTool.create())           // Shell 命令
    .build();

// Agent 可以自动创建任务列表并执行
agent.call("帮我完成以下任务：1. 创建项目目录 2. 初始化 git 仓库 3. 创建 README");
```

### 工具优先级

Agent 应该优先使用专用工具：

| 操作 | 推荐工具 | 不推荐 |
|------|---------|--------|
| 读取文件 | read_file | bash cat |
| 编辑文件 | edit_file | bash sed |
| 写入文件 | write_file | bash echo > |
| 搜索文件 | glob_files | bash find |
| 搜索内容 | grep | bash grep |
| 执行命令 | bash | — |

## 与上节对比

| 维度 | 第 13 节 | 第 14 节 |
|------|---------|---------|
| 工具能力 | 基础工具 | + TodoWrite, Bash |
| 任务规划 | 无 | + 显式任务列表 |
| 系统交互 | 无 | + Shell 命令 |

## 本节小结

✅ 理解高级内置工具的设计模式
✅ 掌握 TodoWrite 任务管理工具
✅ 掌握 Bash Shell 命令执行工具
✅ 能够为 Agent 添加实用工具增强能力
