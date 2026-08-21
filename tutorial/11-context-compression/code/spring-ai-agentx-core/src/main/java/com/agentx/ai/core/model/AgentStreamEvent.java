package com.agentx.ai.core.model;

import com.agentx.ai.core.exception.AgentErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Agent 流式事件。
 * <p>
 * 本节新增：Thinking 事件 — 思考模型的推理过程。
 *
 * @author bigchui
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AgentStreamEvent.Text.class, name = "Text"),
    @JsonSubTypes.Type(value = AgentStreamEvent.Thinking.class, name = "Thinking"),
    @JsonSubTypes.Type(value = AgentStreamEvent.ToolStart.class, name = "ToolStart"),
    @JsonSubTypes.Type(value = AgentStreamEvent.ToolEnd.class, name = "ToolEnd"),
    @JsonSubTypes.Type(value = AgentStreamEvent.Error.class, name = "Error"),
    @JsonSubTypes.Type(value = AgentStreamEvent.Complete.class, name = "Complete")
})
public sealed interface AgentStreamEvent permits
        AgentStreamEvent.Text,
        AgentStreamEvent.Thinking,
        AgentStreamEvent.ToolStart,
        AgentStreamEvent.ToolEnd,
        AgentStreamEvent.Error,
        AgentStreamEvent.Complete {

    /**
     * LLM 正常文本输出。
     */
    record Text(String content) implements AgentStreamEvent {
    }

    /**
     * 思考模型的推理过程（本节新增）。
     *
     * @param content 思考内容
     */
    record Thinking(String content) implements AgentStreamEvent {
    }

    /**
     * 工具即将执行。
     */
    record ToolStart(String toolName, String toolCallId, String arguments) implements AgentStreamEvent {
    }

    /**
     * 工具执行完成。
     */
    record ToolEnd(String toolName, String toolCallId, String result) implements AgentStreamEvent {
    }

    /**
     * LLM 调用异常事件。
     */
    record Error(AgentErrorCode code, String message, String detail) implements AgentStreamEvent {
    }

    /**
     * Agent 执行完成。
     */
    record Complete(long totalPromptTokens, long totalCompletionTokens) implements AgentStreamEvent {
        public Complete() {
            this(0, 0);
        }
    }
}
