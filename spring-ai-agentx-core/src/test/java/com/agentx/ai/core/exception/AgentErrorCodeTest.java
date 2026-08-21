package com.agentx.ai.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentErrorCode 单元测试。
 */
@DisplayName("AgentErrorCode 测试")
class AgentErrorCodeTest {

    @Test
    @DisplayName("错误码数量")
    void shouldHaveCorrectCount() {
        assertEquals(6, AgentErrorCode.values().length);
    }

    @Nested
    @DisplayName("错误码值测试")
    class CodeValueTests {

        @Test
        @DisplayName("LLM_CALL_FAILED 错误码")
        void shouldHaveLlmCallFailedCode() {
            assertEquals("E1001", AgentErrorCode.LLM_CALL_FAILED.code());
        }

        @Test
        @DisplayName("LLM_EMPTY_RESPONSE 错误码")
        void shouldHaveLlmEmptyResponseCode() {
            assertEquals("E1002", AgentErrorCode.LLM_EMPTY_RESPONSE.code());
        }

        @Test
        @DisplayName("CONCURRENT_EXECUTION 错误码")
        void shouldHaveConcurrentExecutionCode() {
            assertEquals("E2001", AgentErrorCode.CONCURRENT_EXECUTION.code());
        }

        @Test
        @DisplayName("SANDBOX_IMAGE_PULL_FAILED 错误码")
        void shouldHaveSandboxImagePullFailedCode() {
            assertEquals("E3001", AgentErrorCode.SANDBOX_IMAGE_PULL_FAILED.code());
        }

        @Test
        @DisplayName("SANDBOX_EXEC_TIMEOUT 错误码")
        void shouldHaveSandboxExecTimeoutCode() {
            assertEquals("E3002", AgentErrorCode.SANDBOX_EXEC_TIMEOUT.code());
        }

        @Test
        @DisplayName("SANDBOX_CONTAINER_NOT_FOUND 错误码")
        void shouldHaveSandboxContainerNotFoundCode() {
            assertEquals("E3003", AgentErrorCode.SANDBOX_CONTAINER_NOT_FOUND.code());
        }
    }

    @Nested
    @DisplayName("valueOf 测试")
    class ValueOfTests {

        @Test
        @DisplayName("valueOf 正确解析")
        void shouldSupportValueOf() {
            assertEquals(AgentErrorCode.LLM_CALL_FAILED, AgentErrorCode.valueOf("LLM_CALL_FAILED"));
            assertEquals(AgentErrorCode.CONCURRENT_EXECUTION, AgentErrorCode.valueOf("CONCURRENT_EXECUTION"));
        }
    }
}
