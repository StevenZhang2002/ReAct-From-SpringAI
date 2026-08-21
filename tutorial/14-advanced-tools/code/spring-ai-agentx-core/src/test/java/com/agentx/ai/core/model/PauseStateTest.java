package com.agentx.ai.core.model;

import com.agentx.ai.core.interrupt.PauseReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PauseState 单元测试（第09节）。
 *
 * <p>测试 Builder 模式、暂停状态快照。
 */
@DisplayName("PauseState 测试")
class PauseStateTest {

    @Nested
    @DisplayName("Builder 模式测试")
    class BuilderTests {

        @Test
        @DisplayName("使用 Builder 创建完整 PauseState")
        void shouldBuildCompletePauseState() {
            // Given
            List<Message> messages = List.of(new UserMessage("测试"));
            List<PendingToolCall> pendingToolCalls = List.of(
                    new PendingToolCall("id1", "tool1", "{}")
            );
            RunnableParams params = RunnableParams.empty();

            // When
            PauseState state = PauseState.builder()
                    .messages(messages)
                    .currentRound(3)
                    .pendingToolCalls(pendingToolCalls)
                    .params(params)
                    .query("测试问题")
                    .sessionId(12345L)
                    .totalPromptTokens(100)
                    .totalCompletionTokens(50)
                    .reason(PauseReason.HITL_TOOL_REQUEST)
                    .build();

            // Then
            assertEquals(messages, state.getMessages());
            assertEquals(3, state.getCurrentRound());
            assertEquals(pendingToolCalls, state.getPendingToolCalls());
            assertEquals(params, state.getParams());
            assertEquals("测试问题", state.getQuery());
            assertEquals(12345L, state.getSessionId());
            assertEquals(100, state.getTotalPromptTokens());
            assertEquals(50, state.getTotalCompletionTokens());
            assertEquals(PauseReason.HITL_TOOL_REQUEST, state.getReason());
        }

        @Test
        @DisplayName("pendingToolCalls 为 null 时返回空列表")
        void shouldReturnEmptyListWhenPendingToolCallsIsNull() {
            // When
            PauseState state = PauseState.builder()
                    .pendingToolCalls(null)
                    .build();

            // Then
            assertNotNull(state.getPendingToolCalls());
            assertTrue(state.getPendingToolCalls().isEmpty());
        }
    }

    @Nested
    @DisplayName("Getters 测试")
    class GetterTests {

        @Test
        @DisplayName("获取 messages")
        void shouldGetMessages() {
            // Given
            List<Message> messages = List.of(new UserMessage("你好"));
            PauseState state = PauseState.builder()
                    .messages(messages)
                    .build();

            // Then
            assertEquals(messages, state.getMessages());
        }

        @Test
        @DisplayName("获取 currentRound")
        void shouldGetCurrentRound() {
            // Given
            PauseState state = PauseState.builder()
                    .currentRound(5)
                    .build();

            // Then
            assertEquals(5, state.getCurrentRound());
        }

        @Test
        @DisplayName("获取 query")
        void shouldGetQuery() {
            // Given
            PauseState state = PauseState.builder()
                    .query("用户问题")
                    .build();

            // Then
            assertEquals("用户问题", state.getQuery());
        }

        @Test
        @DisplayName("获取 sessionId")
        void shouldGetSessionId() {
            // Given
            PauseState state = PauseState.builder()
                    .sessionId(999L)
                    .build();

            // Then
            assertEquals(999L, state.getSessionId());
        }

        @Test
        @DisplayName("获取 reason")
        void shouldGetReason() {
            // Given
            PauseState state = PauseState.builder()
                    .reason(PauseReason.USER_INTERRUPT)
                    .build();

            // Then
            assertEquals(PauseReason.USER_INTERRUPT, state.getReason());
        }
    }

    @Nested
    @DisplayName("Token 用量测试")
    class TokenUsageTests {

        @Test
        @DisplayName("获取 totalPromptTokens")
        void shouldGetTotalPromptTokens() {
            // Given
            PauseState state = PauseState.builder()
                    .totalPromptTokens(200)
                    .build();

            // Then
            assertEquals(200, state.getTotalPromptTokens());
        }

        @Test
        @DisplayName("获取 totalCompletionTokens")
        void shouldGetTotalCompletionTokens() {
            // Given
            PauseState state = PauseState.builder()
                    .totalCompletionTokens(150)
                    .build();

            // Then
            assertEquals(150, state.getTotalCompletionTokens());
        }
    }

    @Nested
    @DisplayName("PendingToolCalls 测试")
    class PendingToolCallsTests {

        @Test
        @DisplayName("获取 pendingToolCalls")
        void shouldGetPendingToolCalls() {
            // Given
            List<PendingToolCall> pendingToolCalls = List.of(
                    new PendingToolCall("id1", "tool1", "{}"),
                    new PendingToolCall("id2", "tool2", "{\"key\":\"value\"}")
            );
            PauseState state = PauseState.builder()
                    .pendingToolCalls(pendingToolCalls)
                    .build();

            // Then
            assertEquals(2, state.getPendingToolCalls().size());
            assertEquals("tool1", state.getPendingToolCalls().get(0).name());
            assertEquals("tool2", state.getPendingToolCalls().get(1).name());
        }

        @Test
        @DisplayName("空 pendingToolCalls")
        void shouldHandleEmptyPendingToolCalls() {
            // Given
            PauseState state = PauseState.builder()
                    .pendingToolCalls(List.of())
                    .build();

            // Then
            assertTrue(state.getPendingToolCalls().isEmpty());
        }
    }
}
