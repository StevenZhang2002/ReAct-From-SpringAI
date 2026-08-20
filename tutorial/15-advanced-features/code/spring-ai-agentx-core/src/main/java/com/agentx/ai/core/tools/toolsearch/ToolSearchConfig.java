package com.agentx.ai.core.tools.toolsearch;

/**
 * 工具搜索配置。
 *
 * @author bigchui
 */
public record ToolSearchConfig(
        Mode mode,
        int maxResults
) {

    public enum Mode {
        KEYWORD, LLM, HYBRID
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Mode mode = Mode.KEYWORD;
        private int maxResults = 3;

        public Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder maxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public ToolSearchConfig build() {
            return new ToolSearchConfig(mode, maxResults);
        }
    }
}
