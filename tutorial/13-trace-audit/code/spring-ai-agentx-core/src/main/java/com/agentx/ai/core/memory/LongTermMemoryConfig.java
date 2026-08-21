package com.agentx.ai.core.memory;

import org.springframework.ai.vectorstore.VectorStore;

import java.util.Objects;

/**
 * 长期记忆配置。
 * <p>
 * 封装 VectorStore 与检索参数，传入即启用长期记忆。
 *
 * @author bigchui
 */
public class LongTermMemoryConfig {

    public static final int DEFAULT_TOP_K = 5;
    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.5;

    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;

    private LongTermMemoryConfig(Builder b) {
        this.vectorStore = b.vectorStore;
        this.topK = b.topK;
        this.similarityThreshold = b.similarityThreshold;
    }

    public static Builder builder() {
        return new Builder();
    }

    public VectorStore getVectorStore() {
        return vectorStore;
    }

    public int getTopK() {
        return topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public static class Builder {
        private VectorStore vectorStore;
        private int topK = DEFAULT_TOP_K;
        private double similarityThreshold = DEFAULT_SIMILARITY_THRESHOLD;

        public Builder vectorStore(VectorStore v) {
            this.vectorStore = v;
            return this;
        }

        public Builder topK(int k) {
            this.topK = k;
            return this;
        }

        public Builder similarityThreshold(double t) {
            this.similarityThreshold = t;
            return this;
        }

        public LongTermMemoryConfig build() {
            Objects.requireNonNull(vectorStore, "vectorStore must not be null");
            return new LongTermMemoryConfig(this);
        }
    }
}
