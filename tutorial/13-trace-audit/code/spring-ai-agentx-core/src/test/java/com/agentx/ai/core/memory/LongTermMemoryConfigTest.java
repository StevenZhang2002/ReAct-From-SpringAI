package com.agentx.ai.core.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LongTermMemoryConfig 单元测试（第12节）。
 *
 * <p>测试 Builder 模式、默认值、参数校验。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LongTermMemoryConfig 测试")
class LongTermMemoryConfigTest {

    @Mock
    private VectorStore vectorStore;

    @Nested
    @DisplayName("默认值测试")
    class DefaultTests {

        @Test
        @DisplayName("默认 topK 值")
        void shouldHaveDefaultTopK() {
            assertEquals(5, LongTermMemoryConfig.DEFAULT_TOP_K);
        }

        @Test
        @DisplayName("默认 similarityThreshold 值")
        void shouldHaveDefaultSimilarityThreshold() {
            assertEquals(0.5, LongTermMemoryConfig.DEFAULT_SIMILARITY_THRESHOLD, 0.001);
        }
    }

    @Nested
    @DisplayName("Builder 模式测试")
    class BuilderTests {

        @Test
        @DisplayName("使用 Builder 创建完整配置")
        void shouldBuildCompleteConfig() {
            // When
            LongTermMemoryConfig config = LongTermMemoryConfig.builder()
                    .vectorStore(vectorStore)
                    .topK(10)
                    .similarityThreshold(0.7)
                    .build();

            // Then
            assertEquals(vectorStore, config.getVectorStore());
            assertEquals(10, config.getTopK());
            assertEquals(0.7, config.getSimilarityThreshold(), 0.001);
        }

        @Test
        @DisplayName("Builder 使用默认值")
        void shouldUseDefaultValues() {
            // When
            LongTermMemoryConfig config = LongTermMemoryConfig.builder()
                    .vectorStore(vectorStore)
                    .build();

            // Then
            assertEquals(LongTermMemoryConfig.DEFAULT_TOP_K, config.getTopK());
            assertEquals(LongTermMemoryConfig.DEFAULT_SIMILARITY_THRESHOLD,
                    config.getSimilarityThreshold(), 0.001);
        }

        @Test
        @DisplayName("vectorStore 为 null 时抛出异常")
        void shouldThrowWhenVectorStoreIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                    LongTermMemoryConfig.builder().build());
        }

        @Test
        @DisplayName("Builder 部分覆盖")
        void shouldPartiallyOverride() {
            // When
            LongTermMemoryConfig config = LongTermMemoryConfig.builder()
                    .vectorStore(vectorStore)
                    .topK(20)
                    .build();

            // Then
            assertEquals(20, config.getTopK());
            assertEquals(LongTermMemoryConfig.DEFAULT_SIMILARITY_THRESHOLD,
                    config.getSimilarityThreshold(), 0.001);
        }
    }

    @Nested
    @DisplayName("Getters 测试")
    class GetterTests {

        @Test
        @DisplayName("获取 vectorStore")
        void shouldGetVectorStore() {
            // Given
            LongTermMemoryConfig config = LongTermMemoryConfig.builder()
                    .vectorStore(vectorStore)
                    .build();

            // Then
            assertEquals(vectorStore, config.getVectorStore());
        }

        @Test
        @DisplayName("获取 topK")
        void shouldGetTopK() {
            // Given
            LongTermMemoryConfig config = LongTermMemoryConfig.builder()
                    .vectorStore(vectorStore)
                    .topK(15)
                    .build();

            // Then
            assertEquals(15, config.getTopK());
        }

        @Test
        @DisplayName("获取 similarityThreshold")
        void shouldGetSimilarityThreshold() {
            // Given
            LongTermMemoryConfig config = LongTermMemoryConfig.builder()
                    .vectorStore(vectorStore)
                    .similarityThreshold(0.8)
                    .build();

            // Then
            assertEquals(0.8, config.getSimilarityThreshold(), 0.001);
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("topK 为 1")
        void shouldHandleTopKOne() {
            // When
            LongTermMemoryConfig config = LongTermMemoryConfig.builder()
                    .vectorStore(vectorStore)
                    .topK(1)
                    .build();

            // Then
            assertEquals(1, config.getTopK());
        }

        @Test
        @DisplayName("similarityThreshold 为 0")
        void shouldHandleZeroThreshold() {
            // When
            LongTermMemoryConfig config = LongTermMemoryConfig.builder()
                    .vectorStore(vectorStore)
                    .similarityThreshold(0.0)
                    .build();

            // Then
            assertEquals(0.0, config.getSimilarityThreshold(), 0.001);
        }

        @Test
        @DisplayName("similarityThreshold 为 1")
        void shouldHandleOneThreshold() {
            // When
            LongTermMemoryConfig config = LongTermMemoryConfig.builder()
                    .vectorStore(vectorStore)
                    .similarityThreshold(1.0)
                    .build();

            // Then
            assertEquals(1.0, config.getSimilarityThreshold(), 0.001);
        }
    }
}
