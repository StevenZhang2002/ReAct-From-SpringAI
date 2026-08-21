# 第 10 节：Hook 生命周期

> 📚 **这是 Spring AI AgentX 渐进式复现教程的第 10 节**  
> 本教程共 15 节，通过独立 git 分支逐步叠加 feature，最终 1:1 复现完整的 Java Agent 框架。  
> 👉 [查看教程目录](../..) | [← 上一节](../09-hitl/README.md) | [下一节 →](../11-context-compression/README.md)

## 本节目标

实现可扩展的 Hook 系统：允许用户在 Agent 执行的关键节点插入自定义逻辑，实现日志记录、参数修改、权限校验等功能。

## 上节回顾

第 09 节实现了 Human-in-the-Loop，Agent 可以暂停等待用户输入。但用户无法在 Agent 执行过程中插入自定义逻辑（如日志、审计、参数修改）。

## 本节新增

- ✅ `HookEvent` — 事件基接口（sealed 接口）
- ✅ `AgentHook` — Hook 接口（实现 `onEvent` 方法）
- ✅ `HookManager` — Hook 管理器（按优先级排序、触发事件）
- ✅ `BeforeToolExecutionEvent` — 工具执行前事件（可修改参数）
- ✅ `AfterToolExecutionEvent` — 工具执行后事件（只读）
- ✅ `BeforeCallEvent` / `AfterCallEvent` — LLM 调用前后事件
- ✅ `ReactAgent` 新增 `hooks` 属性

## 核心设计

### Hook 系统架构

```
Agent 执行流程
  │
  ├── BeforeCallEvent → Hook 可修改 LLM 请求
  │
  ├── LLM 调用
  │
  ├── AfterCallEvent → Hook 可检查 LLM 响应
  │
  ├── 工具执行循环
  │   ├── BeforeToolExecutionEvent → Hook 可修改工具参数
  │   ├── 工具执行
  │   └── AfterToolExecutionEvent → Hook 可检查工具结果
  │
  └── 返回结果
```

### 事件类型

| 事件 | 触发时机 | 可修改 |
|------|---------|--------|
| `BeforeCallEvent` | LLM 调用前 | messages |
| `AfterCallEvent` | LLM 调用后 | 否 |
| `BeforeToolExecutionEvent` | 工具执行前 | arguments, toolContext |
| `AfterToolExecutionEvent` | 工具执行后 | 否 |

### Hook 优先级

```java
public class MyHook implements AgentHook {
    @Override
    public int priority() {
        return 100; // 数值越大越先执行
    }
    
    @Override
    public HookEvent onEvent(HookEvent event) {
        return switch (event) {
            case BeforeToolExecutionEvent e -> {
                // 修改工具参数
                e.setArguments(modifyArgs(e.getArguments()));
                yield e;
            }
            default -> event;
        };
    }
}
```

## 使用示例

```java
// 日志 Hook
AgentHook loggingHook = new AgentHook() {
    @Override
    public HookEvent onEvent(HookEvent event) {
        return switch (event) {
            case BeforeToolExecutionEvent e -> {
                System.out.println("执行工具: " + e.getToolName());
                System.out.println("参数: " + e.getArguments());
                yield e;
            }
            case AfterToolExecutionEvent e -> {
                System.out.println("工具结果: " + e.getResult());
                yield e;
            }
            default -> event;
        };
    }
};

// 参数校验 Hook
AgentHook validationHook = new AgentHook() {
    @Override
    public HookEvent onEvent(HookEvent event) {
        if (event instanceof BeforeToolExecutionEvent e) {
            if (e.getToolName().equals("dangerous_tool")) {
                throw new RuntimeException("禁止使用危险工具");
            }
        }
        return event;
    }
};

ReactAgent agent = ReactAgent.builder()
    .chatModel(chatModel)
    .hooks(loggingHook, validationHook)  // 注册多个 Hook
    .build();
```

## 与上节对比

| 维度 | 第 09 节 | 第 10 节 |
|------|---------|---------|
| 扩展性 | 无 | Hook 系统 |
| 事件类型 | — | 4 种生命周期事件 |
| ReactAgent 属性 | + askUser | + hooks |

## ReactAgent 属性增长

```
第03节: chatModel, instructions
第04节: + maxRounds, tools
第05节: + stream() 方法
第06节: + dataSource, enableSession
第07节: + taskManager
第08节: + thinkingMode
第09节: + askUser
第10节: + hooks  ← 本节
```

## 本节小结

✅ 理解 Hook 系统的设计模式
✅ 掌握 sealed 接口实现类型安全的事件
✅ 实现 Hook 优先级和链式调用
✅ 能够通过 Hook 拦截和修改 Agent 行为
