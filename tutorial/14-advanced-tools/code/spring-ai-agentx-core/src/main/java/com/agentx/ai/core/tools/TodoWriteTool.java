package com.agentx.ai.core.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

/**
 * 任务列表管理工具。
 * <p>
 * 使 AI 智能体能创建、追踪和更新任务列表，将隐式规划转为显式、可观测的工作流。
 *
 * @author bigchui
 */
public class TodoWriteTool {

    @Tool(name = "TodoWrite", description = """
        创建和管理结构化任务列表，用于跟踪多步骤任务的进度。

        ## 强制执行规则
        1. 收到多步骤任务时，必须先调用此工具创建任务列表（全部 pending），然后再执行任何实际操作
        2. 每开始一个任务前，必须先调用此工具将其标记为 in_progress
        3. 每完成一个任务后，必须立即调用此工具将其标记为 completed

        ## 校验规则
        - 同一时间只能有一个 in_progress 任务
        - content 和 activeForm 不能为空
        - 状态值必须是 pending、in_progress 或 completed
        """)
    public String todoWrite(
            @ToolParam(description = "任务列表，包含所有任务的当前状态") List<TodoItem> todos) {
        validateTodos(todos);
        return "任务列表已成功更新。请使用任务列表跟踪你的进度，并继续执行当前的任务。";
    }

    private void validateTodos(List<TodoItem> todos) {
        if (todos == null || todos.isEmpty()) {
            throw new IllegalArgumentException("任务列表不能为空");
        }

        for (int i = 0; i < todos.size(); i++) {
            TodoItem item = todos.get(i);

            if (item == null) {
                throw new IllegalArgumentException("索引 " + i + " 处的任务为 null");
            }
            if (item.content() == null || item.content().isBlank()) {
                throw new IllegalArgumentException(
                        "索引 " + i + " 处的任务 content 为空或空白");
            }
            if (item.activeForm() == null || item.activeForm().isBlank()) {
                throw new IllegalArgumentException("索引 " + i + " 处的任务 activeForm 为空或空白");
            }
            if (item.status() == null) {
                throw new IllegalArgumentException("索引 " + i + " 处的任务 status 为 null");
            }
        }

        long inProgressCount = todos.stream()
                .filter(item -> item.status() == Status.in_progress)
                .count();

        if (inProgressCount > 1) {
            throw new IllegalArgumentException("同一时间只能有一个任务处于 in_progress 状态。当前有 " + inProgressCount
                    + " 个 in_progress 任务。");
        }
    }

    // ==================== 数据模型 ====================

    public record TodoItem(
            @ToolParam(description = "任务内容，祈使形式（如\"运行测试\"）") String content,
            @ToolParam(description = "任务状态：pending（未开始）、in_progress（执行中）、completed（已完成）") Status status,
            @ToolParam(description = "执行时显示的现在进行时形式（如\"正在运行测试\"）") String activeForm) {
    }

    public enum Status {
        pending, in_progress, completed
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建 TodoWriteTool。
     */
    public static ToolCallback[] create() {
        return ToolCallbacks.from(new TodoWriteTool());
    }
}
