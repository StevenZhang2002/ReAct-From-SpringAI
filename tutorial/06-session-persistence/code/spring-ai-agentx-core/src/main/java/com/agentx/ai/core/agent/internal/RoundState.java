package com.agentx.ai.core.agent.internal;

/**
 * 轮次状态 — 跟踪一轮 LLM 流式响应中累积的数据。
 * <p>
 * 每个 chunk 到达时更新对应字段，流结束时根据此状态判断本轮结果。
 *
 * @author bigchui
 */
public class RoundState {

    /** 累积的文本内容 */
    public final StringBuilder textBuffer = new StringBuilder();

    /** 累积的工具调用（按 id 合并 arguments） */
    public final java.util.List<org.springframework.ai.chat.messages.AssistantMessage.ToolCall> toolCalls
            = new java.util.ArrayList<>();

    /** 当前轮模式：TEXT 或 TOOL_CALL */
    public RoundMode mode = RoundMode.TEXT;

    /** 完成原因 */
    public String finishReason;

    /** token 用量 */
    public long promptTokens = -1;
    public long completionTokens;
}
