package com.agentx.ai.core.hook;

import org.springframework.ai.chat.model.ChatResponse;

/**
 * LLM 调用后事件（只读）。
 *
 * @author bigchui
 */
public final class AfterCallEvent implements HookEvent {

    private final String query;
    private final ChatResponse response;
    private final long durationMs;

    public AfterCallEvent(String query, ChatResponse response, long durationMs) {
        this.query = query;
        this.response = response;
        this.durationMs = durationMs;
    }

    public String getQuery() {
        return query;
    }

    public ChatResponse getResponse() {
        return response;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
