package com.agentx.ai.core.agent.internal;

import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 轮次状态 — 跟踪一轮 LLM 流式响应中累积的数据。
 * <p>
 * 本节新增：reasoningBuffer（思考内容缓冲）、inThink（标签状态追踪）。
 *
 * @author bigchui
 */
public class RoundState {

    /** 累积的文本内容 */
    public final StringBuilder textBuffer = new StringBuilder();

    /** 累积的思考内容（本节新增） */
    public final StringBuilder reasoningBuffer = new StringBuilder();

    /** 累积的工具调用 */
    public final List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();

    /** 当前轮模式 */
    public RoundMode mode = RoundMode.TEXT;

    /** think 标签内状态（跨 chunk 追踪，本节新增） */
    public boolean inThink = false;

    /** token 用量 */
    public long promptTokens = -1;
    public long completionTokens;
}
