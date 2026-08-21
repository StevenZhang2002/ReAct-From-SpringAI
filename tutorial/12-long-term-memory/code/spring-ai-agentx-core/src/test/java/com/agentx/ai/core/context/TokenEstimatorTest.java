package com.agentx.ai.core.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TokenEstimator 单元测试（第11节）。
 *
 * <p>测试中英文混合 token 估算。
 */
@DisplayName("TokenEstimator 测试")
class TokenEstimatorTest {

    @Nested
    @DisplayName("estimateTokens(String) 测试")
    class StringTests {

        @Test
        @DisplayName("估算纯英文文本")
        void shouldEstimateEnglishText() {
            // Given
            String text = "Hello, world!";

            // When
            int tokens = TokenEstimator.estimateTokens(text);

            // Then
            assertTrue(tokens > 0);
            // 英文约 4 字符 = 1 token，13 字符约 3 token
            assertTrue(tokens >= 2 && tokens <= 5);
        }

        @Test
        @DisplayName("估算纯中文文本")
        void shouldEstimateChineseText() {
            // Given
            String text = "你好世界";

            // When
            int tokens = TokenEstimator.estimateTokens(text);

            // Then
            assertTrue(tokens > 0);
            // 中文约 1.5 字符 = 1 token，4 字符约 2-3 token
            assertTrue(tokens >= 2 && tokens <= 4);
        }

        @Test
        @DisplayName("估算中英文混合文本")
        void shouldEstimateMixedText() {
            // Given
            String text = "Hello 你好 World 世界";

            // When
            int tokens = TokenEstimator.estimateTokens(text);

            // Then
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("估算空字符串")
        void shouldEstimateEmptyString() {
            // When
            int tokens = TokenEstimator.estimateTokens("");

            // Then
            assertEquals(0, tokens);
        }

        @Test
        @DisplayName("估算 null")
        void shouldEstimateNull() {
            // When
            int tokens = TokenEstimator.estimateTokens((String) null);

            // Then
            assertEquals(0, tokens);
        }
    }

    @Nested
    @DisplayName("estimateTokens(Message) 测试")
    class MessageTests {

        @Test
        @DisplayName("估算 UserMessage")
        void shouldEstimateUserMessage() {
            // Given
            Message message = new UserMessage("你好世界");

            // When
            int tokens = TokenEstimator.estimateTokens(message);

            // Then
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("估算 SystemMessage")
        void shouldEstimateSystemMessage() {
            // Given
            Message message = new SystemMessage("你是一个助手");

            // When
            int tokens = TokenEstimator.estimateTokens(message);

            // Then
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("估算 AssistantMessage")
        void shouldEstimateAssistantMessage() {
            // Given
            Message message = AssistantMessage.builder()
                    .content("这是回答")
                    .build();

            // When
            int tokens = TokenEstimator.estimateTokens(message);

            // Then
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("估算 null Message")
        void shouldEstimateNullMessage() {
            // When
            int tokens = TokenEstimator.estimateTokens((Message) null);

            // Then
            assertEquals(0, tokens);
        }
    }

    @Nested
    @DisplayName("estimateTokens(List<Message>) 测试")
    class MessageListTests {

        @Test
        @DisplayName("估算消息列表")
        void shouldEstimateMessageList() {
            // Given
            List<Message> messages = List.of(
                    new SystemMessage("你是助手"),
                    new UserMessage("你好"),
                    AssistantMessage.builder().content("你好！").build()
            );

            // When
            int tokens = TokenEstimator.estimateTokens(messages);

            // Then
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("估算空列表")
        void shouldEstimateEmptyList() {
            // When
            int tokens = TokenEstimator.estimateTokens(List.of());

            // Then
            assertEquals(0, tokens);
        }

        @Test
        @DisplayName("估算 null 列表")
        void shouldEstimateNullList() {
            // When
            int tokens = TokenEstimator.estimateTokens((List<Message>) null);

            // Then
            assertEquals(0, tokens);
        }

        @Test
        @DisplayName("长文本估算")
        void shouldEstimateLongText() {
            // Given
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("测试文本");
            }
            List<Message> messages = List.of(new UserMessage(sb.toString()));

            // When
            int tokens = TokenEstimator.estimateTokens(messages);

            // Then
            assertTrue(tokens > 100);
        }
    }

    @Nested
    @DisplayName("CJK 字符检测测试")
    class CJKDetectionTests {

        @Test
        @DisplayName("基本汉字")
        void shouldDetectBasicCJK() {
            // Given
            String text = "中文测试";

            // When
            int tokens = TokenEstimator.estimateTokens(text);

            // Then
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("日文平假名")
        void shouldDetectHiragana() {
            // Given
            String text = "こんにちは";

            // When
            int tokens = TokenEstimator.estimateTokens(text);

            // Then
            assertTrue(tokens > 0);
        }

        @Test
        @DisplayName("日文片假名")
        void shouldDetectKatakana() {
            // Given
            String text = "コンニチハ";

            // When
            int tokens = TokenEstimator.estimateTokens(text);

            // Then
            assertTrue(tokens > 0);
        }
    }
}
