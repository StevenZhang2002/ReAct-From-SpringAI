package com.agentx.ai.core.agent.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoundState 单元测试（第05节）。
 *
 * <p>测试流式响应中的状态累积。
 */
@DisplayName("RoundState 测试")
class RoundStateTest {

    @Nested
    @DisplayName("初始状态测试")
    class InitialStateTests {

        @Test
        @DisplayName("初始模式为 TEXT")
        void shouldHaveTextModeInitially() {
            // When
            RoundState state = new RoundState();

            // Then
            assertEquals(RoundMode.TEXT, state.mode);
        }

        @Test
        @DisplayName("初始文本缓冲区为空")
        void shouldHaveEmptyTextBufferInitially() {
            // When
            RoundState state = new RoundState();

            // Then
            assertEquals(0, state.textBuffer.length());
            assertTrue(state.textBuffer.isEmpty());
        }

        @Test
        @DisplayName("初始工具调用列表为空")
        void shouldHaveEmptyToolCallsInitially() {
            // When
            RoundState state = new RoundState();

            // Then
            assertTrue(state.toolCalls.isEmpty());
        }

        @Test
        @DisplayName("初始完成原因为 null")
        void shouldHaveNullFinishReasonInitially() {
            // When
            RoundState state = new RoundState();

            // Then
            assertNull(state.finishReason);
        }

        @Test
        @DisplayName("初始 token 用量")
        void shouldHaveInitialTokenCounts() {
            // When
            RoundState state = new RoundState();

            // Then
            assertEquals(-1, state.promptTokens);
            assertEquals(0, state.completionTokens);
        }
    }

    @Nested
    @DisplayName("文本累积测试")
    class TextAccumulationTests {

        @Test
        @DisplayName("累积文本内容")
        void shouldAccumulateText() {
            // Given
            RoundState state = new RoundState();

            // When
            state.textBuffer.append("你好");
            state.textBuffer.append("，");
            state.textBuffer.append("世界");

            // Then
            assertEquals("你好，世界", state.textBuffer.toString());
        }

        @Test
        @DisplayName("文本缓冲区可变")
        void shouldHaveMutableTextBuffer() {
            // Given
            RoundState state = new RoundState();

            // When
            state.textBuffer.append("第一部分");
            int length1 = state.textBuffer.length();
            state.textBuffer.append("第二部分");
            int length2 = state.textBuffer.length();

            // Then
            assertEquals(4, length1);
            assertEquals(8, length2);
        }
    }

    @Nested
    @DisplayName("工具调用累积测试")
    class ToolCallAccumulationTests {

        @Test
        @DisplayName("添加工具调用")
        void shouldAddToolCalls() {
            // Given
            RoundState state = new RoundState();

            AssistantMessage.ToolCall toolCall = AssistantMessage.ToolCall.builder()
                    .id("call_123")
                    .name("getWeather")
                    .arguments("{\"city\": \"北京\"}")
                    .build();

            // When
            state.toolCalls.add(toolCall);

            // Then
            assertEquals(1, state.toolCalls.size());
            assertEquals("getWeather", state.toolCalls.get(0).name());
        }

        @Test
        @DisplayName("添加多个工具调用")
        void shouldAddMultipleToolCalls() {
            // Given
            RoundState state = new RoundState();

            AssistantMessage.ToolCall call1 = AssistantMessage.ToolCall.builder()
                    .id("call_1")
                    .name("tool1")
                    .arguments("{}")
                    .build();

            AssistantMessage.ToolCall call2 = AssistantMessage.ToolCall.builder()
                    .id("call_2")
                    .name("tool2")
                    .arguments("{}")
                    .build();

            // When
            state.toolCalls.add(call1);
            state.toolCalls.add(call2);

            // Then
            assertEquals(2, state.toolCalls.size());
        }
    }

    @Nested
    @DisplayName("模式切换测试")
    class ModeSwitchTests {

        @Test
        @DisplayName("切换到 TOOL_CALL 模式")
        void shouldSwitchToToolCallMode() {
            // Given
            RoundState state = new RoundState();
            assertEquals(RoundMode.TEXT, state.mode);

            // When
            state.mode = RoundMode.TOOL_CALL;

            // Then
            assertEquals(RoundMode.TOOL_CALL, state.mode);
        }

        @Test
        @DisplayName("切换回 TEXT 模式")
        void shouldSwitchBackToTextMode() {
            // Given
            RoundState state = new RoundState();
            state.mode = RoundMode.TOOL_CALL;

            // When
            state.mode = RoundMode.TEXT;

            // Then
            assertEquals(RoundMode.TEXT, state.mode);
        }
    }

    @Nested
    @DisplayName("完成原因测试")
    class FinishReasonTests {

        @Test
        @DisplayName("设置完成原因")
        void shouldSetFinishReason() {
            // Given
            RoundState state = new RoundState();

            // When
            state.finishReason = "stop";

            // Then
            assertEquals("stop", state.finishReason);
        }

        @Test
        @DisplayName("设置 tool_calls 完成原因")
        void shouldSetToolCallsFinishReason() {
            // Given
            RoundState state = new RoundState();

            // When
            state.finishReason = "tool_calls";

            // Then
            assertEquals("tool_calls", state.finishReason);
        }
    }

    @Nested
    @DisplayName("Token 用量测试")
    class TokenUsageTests {

        @Test
        @DisplayName("设置 prompt tokens")
        void shouldSetPromptTokens() {
            // Given
            RoundState state = new RoundState();

            // When
            state.promptTokens = 100;

            // Then
            assertEquals(100, state.promptTokens);
        }

        @Test
        @DisplayName("设置 completion tokens")
        void shouldSetCompletionTokens() {
            // Given
            RoundState state = new RoundState();

            // When
            state.completionTokens = 50;

            // Then
            assertEquals(50, state.completionTokens);
        }
    }
}
