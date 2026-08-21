package com.agentx.ai.core.hook;

/**
 * 单个工具执行后事件（callback.call 返回后）。
 *
 * @author bigchui
 */
public final class AfterToolExecutionEvent implements HookEvent {

    private final String toolName;
    private final String toolCallId;
    private final String arguments;
    private final String result;
    private final boolean success;
    private final long durationMs;

    public AfterToolExecutionEvent(String toolName, String toolCallId,
                                   String arguments, String result, boolean success, long durationMs) {
        this.toolName = toolName;
        this.toolCallId = toolCallId;
        this.arguments = arguments;
        this.result = result;
        this.success = success;
        this.durationMs = durationMs;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getArguments() {
        return arguments;
    }

    public String getResult() {
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
