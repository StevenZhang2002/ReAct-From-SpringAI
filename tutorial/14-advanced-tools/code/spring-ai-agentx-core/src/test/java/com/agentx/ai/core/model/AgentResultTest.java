package com.agentx.ai.core.model;

import com.agentx.ai.core.exception.AgentErrorCode;
import com.agentx.ai.core.exception.AgentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentResult 单元测试。
 *
 * <p>测试 sealed 接口、模式匹配、Completed 和 Failed 两种状态。
 */
@DisplayName("AgentResult 测试")
class AgentResultTest {

    @Nested
    @DisplayName("Completed 状态测试")
    class CompletedTests {

        @Test
        @DisplayName("创建 Completed 结果 - 带思考内容")
        void shouldCreateCompletedWithThink() {
            // Given
            String answer = "这是最终答案";
            String think = "这是思考过程";

            // When
            AgentResult result = new AgentResult.Completed(answer, think);

            // Then
            assertInstanceOf(AgentResult.Completed.class, result);
            assertFalse(result.isFailed());
            assertEquals(answer, result.answer());
            assertEquals(think, result.think());
        }

        @Test
        @DisplayName("创建 Completed 结果 - 无思考内容")
        void shouldCreateCompletedWithoutThink() {
            // Given
            String answer = "这是最终答案";

            // When
            AgentResult result = new AgentResult.Completed(answer);

            // Then
            assertInstanceOf(AgentResult.Completed.class, result);
            assertFalse(result.isFailed());
            assertEquals(answer, result.answer());
            assertNull(result.think());
        }

        @Test
        @DisplayName("模式匹配 - Completed")
        void shouldMatchCompletedInPatternMatching() {
            // Given
            AgentResult result = new AgentResult.Completed("答案", "思考");

            // When & Then
            switch (result) {
                case AgentResult.Completed c -> {
                    assertEquals("答案", c.answer());
                    assertEquals("思考", c.think());
                }
                case AgentResult.Failed f -> fail("应该是 Completed 状态");
            }
        }
    }

    @Nested
    @DisplayName("Failed 状态测试")
    class FailedTests {

        @Test
        @DisplayName("创建 Failed 结果")
        void shouldCreateFailed() {
            // Given
            String error = "LLM 调用失败";
            AgentErrorCode code = AgentErrorCode.LLM_CALL_FAILED;

            // When
            AgentResult result = new AgentResult.Failed(error, code);

            // Then
            assertInstanceOf(AgentResult.Failed.class, result);
            assertTrue(result.isFailed());
        }

        @Test
        @DisplayName("Failed 状态调用 answer() 抛出异常")
        void shouldThrowExceptionWhenGetAnswerOnFailed() {
            // Given
            AgentResult result = new AgentResult.Failed("错误", AgentErrorCode.LLM_CALL_FAILED);

            // When & Then
            Exception exception = assertThrows(AgentException.class, result::answer);
            assertInstanceOf(AgentException.class, exception);
        }

        @Test
        @DisplayName("Failed 状态调用 think() 抛出异常")
        void shouldThrowExceptionWhenGetThinkOnFailed() {
            // Given
            AgentResult result = new AgentResult.Failed("错误", AgentErrorCode.LLM_CALL_FAILED);

            // When & Then
            Exception exception = assertThrows(AgentException.class, result::think);
            assertInstanceOf(AgentException.class, exception);
        }

        @Test
        @DisplayName("模式匹配 - Failed")
        void shouldMatchFailedInPatternMatching() {
            // Given
            AgentResult result = new AgentResult.Failed("错误信息", AgentErrorCode.TIMEOUT);

            // When & Then
            switch (result) {
                case AgentResult.Completed c -> fail("应该是 Failed 状态");
                case AgentResult.Failed f -> {
                    assertEquals("错误信息", f.error());
                    assertEquals(AgentErrorCode.TIMEOUT, f.code());
                }
            }
        }
    }

    @Nested
    @DisplayName("isFailed() 方法测试")
    class IsFailedTests {

        @Test
        @DisplayName("Completed 的 isFailed() 返回 false")
        void shouldReturnFalseForCompleted() {
            AgentResult result = new AgentResult.Completed("答案");
            assertFalse(result.isFailed());
        }

        @Test
        @DisplayName("Failed 的 isFailed() 返回 true")
        void shouldReturnTrueForFailed() {
            AgentResult result = new AgentResult.Failed("错误", AgentErrorCode.UNKNOWN);
            assertTrue(result.isFailed());
        }
    }
}
