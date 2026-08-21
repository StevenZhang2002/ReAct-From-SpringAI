package com.agentx.ai.core.model;

import com.agentx.ai.core.interrupt.PauseReason;
import com.agentx.ai.core.interrupt.SafePoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PauseState 单元测试（main 分支完整版）。
 *
 * <p>测试 Builder 模式、所有字段、扩展字段（SafePoint、children 等）。
 */
@DisplayName("PauseState 测试")
class PauseStateTest {

    @Nested
    @DisplayName("Builder 完整构建")
    class BuilderTests {

        @Test
        @DisplayName("构建完整 PauseState")
        void shouldBuildComplete() {
            List<Message> messages = List.of(new UserMessage("测试"));
            List<PendingToolCall> toolCalls = List.of(new PendingToolCall("id1", "tool1", "{}"));
            RunnableParams params = RunnableParams.builder().conversationId("conv-1").build();

            PauseState state = PauseState.builder()
                    .messages(messages)
                    .currentRound(3)
                    .pendingToolCalls(toolCalls)
                    .params(params)
                    .query("问题")
                    .sessionId(123L)
                    .totalPromptTokens(100)
                    .totalCompletionTokens(50)
                    .reason(PauseReason.HITL_TOOL_REQUEST)
                    .safePoint(SafePoint.TOOL_EXECUTION)
                    .interruptMessage("用户中断")
                    .interruptedAt(1000L)
                    .build();

            assertEquals(messages, state.getMessages());
            assertEquals(3, state.getCurrentRound());
            assertEquals(toolCalls, state.getPendingToolCalls());
            assertEquals(params, state.getParams());
            assertEquals("问题", state.getQuery());
            assertEquals(123L, state.getSessionId());
            assertEquals(100, state.getTotalPromptTokens());
            assertEquals(50, state.getTotalCompletionTokens());
            assertEquals(PauseReason.HITL_TOOL_REQUEST, state.getReason());
            assertEquals(SafePoint.TOOL_EXECUTION, state.getSafePoint());
            assertEquals("用户中断", state.getInterruptMessage());
            assertEquals(1000L, state.getInterruptedAt());
        }
    }

    @Nested
    @DisplayName("pendingToolCalls 默认值")
    class PendingToolCallsTests {

        @Test
        @DisplayName("null 返回空列表")
        void shouldReturnEmptyListWhenNull() {
            PauseState state = PauseState.builder().build();
            assertNotNull(state.getPendingToolCalls());
            assertTrue(state.getPendingToolCalls().isEmpty());
        }

        @Test
        @DisplayName("有值时返回列表")
        void shouldReturnListWhenSet() {
            List<PendingToolCall> toolCalls = List.of(new PendingToolCall("id1", "tool1", "{}"));
            PauseState state = PauseState.builder().pendingToolCalls(toolCalls).build();
            assertEquals(1, state.getPendingToolCalls().size());
        }
    }

    @Nested
    @DisplayName("children 测试")
    class ChildrenTests {

        @Test
        @DisplayName("children 为 null")
        void shouldReturnNullChildren() {
            PauseState state = PauseState.builder().build();
            assertNull(state.getChildren());
        }

        @Test
        @DisplayName("设置 children")
        void shouldSetChildren() {
            PauseState child = PauseState.builder().query("子任务").build();
            PauseState parent = PauseState.builder()
                    .children(List.of(child))
                    .build();
            assertEquals(1, parent.getChildren().size());
            assertEquals("子任务", parent.getChildren().get(0).getQuery());
        }
    }

    @Nested
    @DisplayName("SafePoint 测试")
    class SafePointTests {

        @Test
        @DisplayName("safePoint 默认 null")
        void shouldDefaultToNull() {
            PauseState state = PauseState.builder().build();
            assertNull(state.getSafePoint());
        }

        @Test
        @DisplayName("设置 LLM_STREAMING")
        void shouldSetLlmStreaming() {
            PauseState state = PauseState.builder().safePoint(SafePoint.LLM_STREAMING).build();
            assertEquals(SafePoint.LLM_STREAMING, state.getSafePoint());
        }

        @Test
        @DisplayName("设置 TOOL_EXECUTION")
        void shouldSetToolExecution() {
            PauseState state = PauseState.builder().safePoint(SafePoint.TOOL_EXECUTION).build();
            assertEquals(SafePoint.TOOL_EXECUTION, state.getSafePoint());
        }
    }
}
