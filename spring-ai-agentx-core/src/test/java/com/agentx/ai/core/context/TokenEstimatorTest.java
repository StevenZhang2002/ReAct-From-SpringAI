package com.agentx.ai.core.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenEstimator 单元测试（main 分支完整版）。
 */
@DisplayName("TokenEstimator 测试")
class TokenEstimatorTest {

    @Nested
    @DisplayName("estimateTokens(String) 测试")
    class StringTests {

        @Test
        @DisplayName("纯英文文本")
        void shouldEstimateEnglishText() {
            int tokens = TokenEstimator.estimateTokens("Hello, world!");
            assertTrue(tokens > 0);
            assertTrue(tokens >= 2 && tokens <= 5);
        }

        @Test
        @DisplayName("纯中文文本")
        void shouldEstimateChineseText() {
            int tokens = TokenEstimator.estimateTokens("你好世界");
            assertTrue(tokens > 0);
            assertTrue(tokens >= 2 && tokens <= 4);
        }

        @Test
        @DisplayName("中英文混合文本")
        void shouldEstimateMixedText() {
            int tokens = TokenEstimator.estimateTokens("Hello 你好 World 世界");
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("空字符串返回 0")
        void shouldReturnZeroForEmpty() {
            assertEquals(0, TokenEstimator.estimateTokens(""));
        }

        @Test
        @DisplayName("null 返回 0")
        void shouldReturnZeroForNull() {
            assertEquals(0, TokenEstimator.estimateTokens((String) null));
        }
    }

    @Nested
    @DisplayName("estimateTokens(Message) 测试")
    class MessageTests {

        @Test
        @DisplayName("UserMessage")
        void shouldEstimateUserMessage() {
            int tokens = TokenEstimator.estimateTokens(new UserMessage("你好"));
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("SystemMessage")
        void shouldEstimateSystemMessage() {
            int tokens = TokenEstimator.estimateTokens(new SystemMessage("你是助手"));
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("AssistantMessage")
        void shouldEstimateAssistantMessage() {
            int tokens = TokenEstimator.estimateTokens(
                    AssistantMessage.builder().content("回答").build());
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("null Message 返回 0")
        void shouldReturnZeroForNullMessage() {
            assertEquals(0, TokenEstimator.estimateTokens((Message) null));
        }
    }

    @Nested
    @DisplayName("estimateTokens(List<Message>) 测试")
    class MessageListTests {

        @Test
        @DisplayName("消息列表")
        void shouldEstimateMessageList() {
            List<Message> messages = List.of(
                    new SystemMessage("你是助手"),
                    new UserMessage("你好"),
                    AssistantMessage.builder().content("你好！").build()
            );
            int tokens = TokenEstimator.estimateTokens(messages);
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("空列表返回 0")
        void shouldReturnZeroForEmptyList() {
            assertEquals(0, TokenEstimator.estimateTokens(List.of()));
        }

        @Test
        @DisplayName("null 列表返回 0")
        void shouldReturnZeroForNullList() {
            assertEquals(0, TokenEstimator.estimateTokens((List<Message>) null));
        }
    }
}
