package com.agentx.ai.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentException 单元测试。
 *
 * <p>测试异常创建、错误码获取、异常链。
 */
@DisplayName("AgentException 测试")
class AgentExceptionTest {

    @Nested
    @DisplayName("基础构造测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用错误码和消息创建异常")
        void shouldCreateWithCodeAndMessage() {
            // Given
            AgentErrorCode code = AgentErrorCode.LLM_CALL_FAILED;
            String message = "LLM 调用失败";

            // When
            AgentException exception = new AgentException(code, message);

            // Then
            assertEquals(code, exception.getCode());
            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("使用错误码、消息和原因创建异常")
        void shouldCreateWithCodeMessageAndCause() {
            // Given
            AgentErrorCode code = AgentErrorCode.NETWORK_ERROR;
            String message = "网络连接失败";
            Throwable cause = new RuntimeException("Connection refused");

            // When
            AgentException exception = new AgentException(code, message, cause);

            // Then
            assertEquals(code, exception.getCode());
            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }
    }

    @Nested
    @DisplayName("错误码测试")
    class ErrorCodeTests {

        @Test
        @DisplayName("获取 LLM_CALL_FAILED 错误码")
        void shouldGetLlmCallFailedCode() {
            // Given
            AgentException exception = new AgentException(
                    AgentErrorCode.LLM_CALL_FAILED, "调用失败");

            // Then
            assertEquals(AgentErrorCode.LLM_CALL_FAILED, exception.getCode());
        }

        @Test
        @DisplayName("获取 TIMEOUT 错误码")
        void shouldGetTimeoutCode() {
            // Given
            AgentException exception = new AgentException(
                    AgentErrorCode.TIMEOUT, "请求超时");

            // Then
            assertEquals(AgentErrorCode.TIMEOUT, exception.getCode());
        }

        @Test
        @DisplayName("获取 TOOL_EXECUTION_FAILED 错误码")
        void shouldGetToolExecutionFailedCode() {
            // Given
            AgentException exception = new AgentException(
                    AgentErrorCode.TOOL_EXECUTION_FAILED, "工具执行失败");

            // Then
            assertEquals(AgentErrorCode.TOOL_EXECUTION_FAILED, exception.getCode());
        }
    }

    @Nested
    @DisplayName("异常继承测试")
    class InheritanceTests {

        @Test
        @DisplayName("AgentException 是 RuntimeException")
        void shouldBeRuntimeException() {
            // Given
            AgentException exception = new AgentException(
                    AgentErrorCode.UNKNOWN, "未知错误");

            // Then
            assertInstanceOf(RuntimeException.class, exception);
        }

        @Test
        @DisplayName("可以抛出 AgentException")
        void shouldBeThrowable() {
            // When & Then
            assertThrows(AgentException.class, () -> {
                throw new AgentException(AgentErrorCode.UNKNOWN, "测试异常");
            });
        }
    }

    @Nested
    @DisplayName("异常链测试")
    class ExceptionChainTests {

        @Test
        @DisplayName("保留原始异常")
        void shouldPreserveOriginalException() {
            // Given
            Exception originalException = new IllegalArgumentException("原始错误");
            AgentException agentException = new AgentException(
                    AgentErrorCode.INVALID_INPUT,
                    "参数无效",
                    originalException);

            // Then
            assertEquals(originalException, agentException.getCause());
            assertInstanceOf(IllegalArgumentException.class, agentException.getCause());
        }

        @Test
        @DisplayName("可以获取异常消息")
        void shouldGetMessage() {
            // Given
            AgentException exception = new AgentException(
                    AgentErrorCode.SESSION_NOT_FOUND, "会话不存在");

            // Then
            assertEquals("会话不存在", exception.getMessage());
        }
    }
}
