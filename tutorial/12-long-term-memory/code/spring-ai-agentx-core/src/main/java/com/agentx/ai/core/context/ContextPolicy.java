package com.agentx.ai.core.context;

/**
 * 上下文压缩策略配置。
 * <p>
 * 控制压缩触发的阈值与保护区大小。
 *
 * @param lastKeep       保护区大小（最近 N 条消息不被压缩）
 * @param msgThreshold   消息数阈值（达到后触发压缩）
 * @param tokenThreshold token 阈值（达到后触发压缩）
 * @author bigchui
 */
public record ContextPolicy(
        int lastKeep,
        int msgThreshold,
        int tokenThreshold
) {

    public static final int DEFAULT_LAST_KEEP = 50;
    public static final int DEFAULT_MSG_THRESHOLD = 100;
    public static final int DEFAULT_TOKEN_THRESHOLD = 90000;

    public static ContextPolicy defaults() {
        return new ContextPolicy(
                DEFAULT_LAST_KEEP,
                DEFAULT_MSG_THRESHOLD,
                DEFAULT_TOKEN_THRESHOLD
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int lastKeep = DEFAULT_LAST_KEEP;
        private int msgThreshold = DEFAULT_MSG_THRESHOLD;
        private int tokenThreshold = DEFAULT_TOKEN_THRESHOLD;

        public Builder lastKeep(int v) { this.lastKeep = v; return this; }
        public Builder msgThreshold(int v) { this.msgThreshold = v; return this; }
        public Builder tokenThreshold(int v) { this.tokenThreshold = v; return this; }

        public ContextPolicy build() {
            return new ContextPolicy(lastKeep, msgThreshold, tokenThreshold);
        }
    }
}
