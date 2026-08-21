package com.agentx.ai.core.agent.internal;

import com.agentx.ai.core.model.RunnableParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolDefinition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ToolCallExecutor 单元测试（第04节）。
 *
 * <p>测试工具执行、结果组装、JSON 校验。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ToolCallExecutor 测试")
class ToolCallExecutorTest {

    @Mock
    private ToolCallback toolCallback;

    @Mock
    private ToolDefinition toolDefinition;

    @Nested
    @DisplayName("构造测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用工具列表创建执行器")
        void shouldCreateWithToolList() {
            // Given
            when(toolCallback.getToolDefinition()).thenReturn(toolDefinition);
            when(toolDefinition.name()).thenReturn("testTool");

            // When
            ToolCallExecutor executor = new ToolCallExecutor(List.of(toolCallback));

            // Then
            assertNotNull(executor);
        }

        @Test
        @DisplayName("使用 null 创建执行器")
        void shouldCreateWithNull() {
            // When
            ToolCallExecutor executor = new ToolCallExecutor(null);

            // Then
            assertNotNull(executor);
        }

        @Test
        @DisplayName("使用空列表创建执行器")
        void shouldCreateWithEmptyList() {
            // When
            ToolCallExecutor executor = new ToolCallExecutor(List.of());

            // Then
            assertNotNull(executor);
        }
    }

    @Nested
    @DisplayName("execute() 方法测试")
    class ExecuteTests {

        @Test
        @DisplayName("执行工具调用")
        void shouldExecuteToolCall() {
            // Given
            when(toolCallback.getToolDefinition()).thenReturn(toolDefinition);
            when(toolDefinition.name()).thenReturn("getWeather");
            when(toolCallback.call(anyString(), any())).thenReturn("晴天，25度");

            ToolCallExecutor executor = new ToolCallExecutor(List.of(toolCallback));

            AssistantMessage.ToolCall toolCall = AssistantMessage.ToolCall.builder()
                    .id("call_123")
                    .name("getWeather")
                    .arguments("{\"city\": \"北京\"}")
                    .build();

            // When
            String result = executor.execute(toolCall, RunnableParams.empty());

            // Then
            assertEquals("晴天，25度", result);
        }

        @Test
        @DisplayName("工具不存在时返回错误")
        void shouldReturnErrorWhenToolNotFound() {
            // Given
            ToolCallExecutor executor = new ToolCallExecutor(List.of());

            AssistantMessage.ToolCall toolCall = AssistantMessage.ToolCall.builder()
                    .id("call_123")
                    .name("nonExistentTool")
                    .arguments("{}")
                    .build();

            // When
            String result = executor.execute(toolCall, RunnableParams.empty());

            // Then
            assertTrue(result.contains("error"));
            assertTrue(result.contains("nonExistentTool"));
        }

        @Test
        @DisplayName("工具参数为空时使用空对象")
        void shouldUseEmptyJsonWhenArgsIsNull() {
            // Given
            when(toolCallback.getToolDefinition()).thenReturn(toolDefinition);
            when(toolDefinition.name()).thenReturn("testTool");
            when(toolCallback.call(eq("{}"), any())).thenReturn("success");

            ToolCallExecutor executor = new ToolCallExecutor(List.of(toolCallback));

            AssistantMessage.ToolCall toolCall = AssistantMessage.ToolCall.builder()
                    .id("call_123")
                    .name("testTool")
                    .arguments(null)
                    .build();

            // When
            String result = executor.execute(toolCall, RunnableParams.empty());

            // Then
            assertEquals("success", result);
        }

        @Test
        @DisplayName("工具执行异常时返回错误")
        void shouldReturnErrorWhenToolThrowsException() {
            // Given
            when(toolCallback.getToolDefinition()).thenReturn(toolDefinition);
            when(toolDefinition.name()).thenReturn("errorTool");
            when(toolCallback.call(anyString(), any())).thenThrow(new RuntimeException("工具执行失败"));

            ToolCallExecutor executor = new ToolCallExecutor(List.of(toolCallback));

            AssistantMessage.ToolCall toolCall = AssistantMessage.ToolCall.builder()
                    .id("call_123")
                    .name("errorTool")
                    .arguments("{}")
                    .build();

            // When
            String result = executor.execute(toolCall, RunnableParams.empty());

            // Then
            assertTrue(result.contains("error"));
            assertTrue(result.contains("工具执行失败"));
        }
    }

    @Nested
    @DisplayName("buildToolResponseMessage() 方法测试")
    class BuildToolResponseMessageTests {

        @Test
        @DisplayName("构建工具响应消息")
        void shouldBuildToolResponseMessage() {
            // Given
            ToolCallExecutor executor = new ToolCallExecutor(List.of());

            AssistantMessage.ToolCall toolCall = AssistantMessage.ToolCall.builder()
                    .id("call_123")
                    .name("getWeather")
                    .arguments("{}")
                    .build();

            String result = "晴天";

            // When
            ToolResponseMessage message = executor.buildToolResponseMessage(toolCall, result);

            // Then
            assertNotNull(message);
            assertEquals(1, message.getResponses().size());
            assertEquals("call_123", message.getResponses().get(0).id());
            assertEquals("getWeather", message.getResponses().get(0).name());
            assertEquals("晴天", message.getResponses().get(0).responseData());
        }
    }

    @Nested
    @DisplayName("sanitizeToolCalls() 方法测试")
    class SanitizeToolCallsTests {

        @Test
        @DisplayName("合法 JSON 参数保持不变")
        void shouldKeepValidJson() {
            // Given
            ToolCallExecutor executor = new ToolCallExecutor(List.of());

            AssistantMessage.ToolCall toolCall = AssistantMessage.ToolCall.builder()
                    .id("call_123")
                    .name("testTool")
                    .arguments("{\"key\": \"value\"}")
                    .build();

            // When
            List<AssistantMessage.ToolCall> result = executor.sanitizeToolCalls(List.of(toolCall));

            // Then
            assertEquals(1, result.size());
            assertEquals("{\"key\": \"value\"}", result.get(0).arguments());
        }

        @Test
        @DisplayName("非法 JSON 参数替换为空对象")
        void shouldReplaceInvalidJsonWithEmptyObject() {
            // Given
            ToolCallExecutor executor = new ToolCallExecutor(List.of());

            AssistantMessage.ToolCall toolCall = AssistantMessage.ToolCall.builder()
                    .id("call_123")
                    .name("testTool")
                    .arguments("not a json")
                    .build();

            // When
            List<AssistantMessage.ToolCall> result = executor.sanitizeToolCalls(List.of(toolCall));

            // Then
            assertEquals(1, result.size());
            assertEquals("{}", result.get(0).arguments());
        }

        @Test
        @DisplayName("空参数替换为空对象")
        void shouldReplaceBlankArgsWithEmptyObject() {
            // Given
            ToolCallExecutor executor = new ToolCallExecutor(List.of());

            AssistantMessage.ToolCall toolCall = AssistantMessage.ToolCall.builder()
                    .id("call_123")
                    .name("testTool")
                    .arguments("   ")
                    .build();

            // When
            List<AssistantMessage.ToolCall> result = executor.sanitizeToolCalls(List.of(toolCall));

            // Then
            assertEquals(1, result.size());
            assertEquals("{}", result.get(0).arguments());
        }
    }
}
