package com.agentx.ai.core.model;

import com.agentx.ai.core.exception.AgentErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Agent 流式事件。
 * <p>
 * 使用 Jackson 多态序列化，每个事件自动携带 {@code "type"} 字段用于类型鉴别。
 * 例如：{@code {"type":"Text","content":"你好"}}
 *
 * <p>使用 sealed 接口实现模式匹配，支持以下事件类型：
 * <ul>
 *   <li>{@link Text} - LLM 正常文本输出</li>
 *   <li>{@link ToolStart} - 工具即将执行</li>
 *   <li>{@link ToolEnd} - 工具执行完成</li>
 *   <li>{@link Error} - LLM 调用异常（重试时发出）</li>
 *   <li>{@link Complete} - Agent 执行完成</li>
 * </ul>
 *
 * @author bigchui
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AgentStreamEvent.Text.class, name = "Text"),
    @JsonSubTypes.Type(value = AgentStreamEvent.ToolStart.class, name = "ToolStart"),
    @JsonSubTypes.Type(value = AgentStreamEvent.ToolEnd.class, name = "ToolEnd"),
    @JsonSubTypes.Type(value = AgentStreamEvent.Error.class, name = "Error"),
    @JsonSubTypes.Type(value = AgentStreamEvent.Complete.class, name = "Complete")
})
public sealed interface AgentStreamEvent permits
        AgentStreamEvent.Text,
        AgentStreamEvent.ToolStart,
        AgentStreamEvent.ToolEnd,
        AgentStreamEvent.Error,
        AgentStreamEvent.Complete {

    /**
     * LLM 正常文本输出。
     *
     * @param content 文本内容
     */
    record Text(String content) implements AgentStreamEvent {
    }

    /**
     * 工具即将执行。
     *
     * @param toolName   工具名称
     * @param toolCallId 工具调用 ID
     * @param arguments  工具调用参数 JSON
     */
    record ToolStart(String toolName, String toolCallId, String arguments) implements AgentStreamEvent {
    }

    /**
     * 工具执行完成。
     *
     * @param toolName   工具名称
     * @param toolCallId 工具调用 ID
     * @param result     工具返回结果
     */
    record ToolEnd(String toolName, String toolCallId, String result) implements AgentStreamEvent {
    }

    /**
     * LLM 调用异常事件（重试时发出）。
     *
     * @param code    错误码
     * @param message 用户友好提示
     * @param detail  异常详细信息
     */
    record Error(AgentErrorCode code, String message, String detail) implements AgentStreamEvent {
    }

    /**
     * Agent 执行完成。
     *
     * @param totalPromptTokens     整个对话总输入 token 数
     * @param totalCompletionTokens 整个对话总输出 token 数
     */
    record Complete(long totalPromptTokens, long totalCompletionTokens) implements AgentStreamEvent {
        public Complete() {
            this(0, 0);
        }
    }
}
