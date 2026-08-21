package com.agentx.ai.core.model;

import com.agentx.ai.core.interrupt.PauseReason;
import org.springframework.ai.chat.messages.Message;

import java.util.Collections;
import java.util.List;

/**
 * Agent 暂停状态快照（简化版）。
 * <p>
 * 包含恢复 Agent 执行所需的所有信息。
 * 后续章节会扩展：SafePoint（中断安全点）、children（SubAgent 快照）等。
 *
 * @author bigchui
 */
public class PauseState {

    private final List<Message> messages;
    private final int currentRound;
    private final List<PendingToolCall> pendingToolCalls;
    private final RunnableParams params;
    private final String query;
    private final long sessionId;
    private final long totalPromptTokens;
    private final long totalCompletionTokens;
    private final PauseReason reason;

    private PauseState(Builder builder) {
        this.messages = builder.messages;
        this.currentRound = builder.currentRound;
        this.pendingToolCalls = builder.pendingToolCalls;
        this.params = builder.params;
        this.query = builder.query;
        this.sessionId = builder.sessionId;
        this.totalPromptTokens = builder.totalPromptTokens;
        this.totalCompletionTokens = builder.totalCompletionTokens;
        this.reason = builder.reason;
    }

    public List<Message> getMessages() { return messages; }
    public int getCurrentRound() { return currentRound; }
    public List<PendingToolCall> getPendingToolCalls() {
        return pendingToolCalls != null ? pendingToolCalls : Collections.emptyList();
    }
    public RunnableParams getParams() { return params; }
    public String getQuery() { return query; }
    public long getSessionId() { return sessionId; }
    public long getTotalPromptTokens() { return totalPromptTokens; }
    public long getTotalCompletionTokens() { return totalCompletionTokens; }
    public PauseReason getReason() { return reason; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private List<Message> messages;
        private int currentRound;
        private List<PendingToolCall> pendingToolCalls;
        private RunnableParams params;
        private String query;
        private long sessionId;
        private long totalPromptTokens;
        private long totalCompletionTokens;
        private PauseReason reason;

        public Builder messages(List<Message> messages) { this.messages = messages; return this; }
        public Builder currentRound(int currentRound) { this.currentRound = currentRound; return this; }
        public Builder pendingToolCalls(List<PendingToolCall> ptc) { this.pendingToolCalls = ptc; return this; }
        public Builder params(RunnableParams params) { this.params = params; return this; }
        public Builder query(String query) { this.query = query; return this; }
        public Builder sessionId(long sessionId) { this.sessionId = sessionId; return this; }
        public Builder totalPromptTokens(long t) { this.totalPromptTokens = t; return this; }
        public Builder totalCompletionTokens(long t) { this.totalCompletionTokens = t; return this; }
        public Builder reason(PauseReason reason) { this.reason = reason; return this; }
        public PauseState build() { return new PauseState(this); }
    }
}
