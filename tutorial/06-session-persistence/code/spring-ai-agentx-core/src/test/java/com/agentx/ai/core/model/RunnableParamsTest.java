package com.agentx.ai.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RunnableParams 单元测试。
 *
 * <p>测试 Builder 模式、参数获取、customParams 和 toolParams 的区别。
 */
@DisplayName("RunnableParams 测试")
class RunnableParamsTest {

    @Nested
    @DisplayName("Builder 模式测试")
    class BuilderTests {

        @Test
        @DisplayName("使用 Builder 创建完整参数")
        void shouldBuildWithAllFields() {
            // Given & When
            RunnableParams params = RunnableParams.builder()
                    .conversationId("conv-123")
                    .userId("user-456")
                    .addParam("language", "zh-CN")
                    .addParam("temperature", 0.7)
                    .addToolParam("userId", "123")
                    .outputType(OutputType.TEXT)
                    .build();

            // Then
            assertEquals("conv-123", params.getConversationId());
            assertEquals("user-456", params.getUserId());
            assertEquals("zh-CN", params.getParam("language"));
            assertEquals(0.7, (double) params.getParam("temperature"), 0.001);
            assertEquals("123", params.getToolParams().get("userId"));
            assertEquals(OutputType.TEXT, params.getOutputType());
        }

        @Test
        @DisplayName("使用 empty() 创建空参数")
        void shouldCreateEmptyParams() {
            // When
            RunnableParams params = RunnableParams.empty();

            // Then
            assertNull(params.getConversationId());
            assertNull(params.getUserId());
            assertTrue(params.getCustomParams().isEmpty());
            assertTrue(params.getToolParams().isEmpty());
            assertNull(params.getOutputType());
        }

        @Test
        @DisplayName("使用 addParams 批量添加参数")
        void shouldAddBatchParams() {
            // Given
            Map<String, Object> batchParams = Map.of(
                    "key1", "value1",
                    "key2", "value2"
            );

            // When
            RunnableParams params = RunnableParams.builder()
                    .addParams(batchParams)
                    .build();

            // Then
            assertEquals("value1", params.getParam("key1"));
            assertEquals("value2", params.getParam("key2"));
        }
    }

    @Nested
    @DisplayName("参数获取测试")
    class ParamGetterTests {

        @Test
        @DisplayName("getParam 获取存在的参数")
        void shouldGetExistingParam() {
            // Given
            RunnableParams params = RunnableParams.builder()
                    .addParam("name", "Alice")
                    .build();

            // When & Then
            assertEquals("Alice", params.getParam("name"));
        }

        @Test
        @DisplayName("getParam 获取不存在的参数返回 null")
        void shouldReturnNullForNonExistingParam() {
            // Given
            RunnableParams params = RunnableParams.empty();

            // When & Then
            assertNull(params.getParam("nonExisting"));
        }

        @Test
        @DisplayName("getParam 带默认值 - 参数存在")
        void shouldGetParamWithDefaultValue() {
            // Given
            RunnableParams params = RunnableParams.builder()
                    .addParam("count", 10)
                    .build();

            // When & Then
            assertEquals(10, (int) params.getParam("count", 0));
        }

        @Test
        @DisplayName("getParam 带默认值 - 参数不存在")
        void shouldReturnDefaultForNonExistingParam() {
            // Given
            RunnableParams params = RunnableParams.empty();

            // When & Then
            assertEquals(20, (int) params.getParam("count", 20));
        }

        @Test
        @DisplayName("hasParam 检查参数存在")
        void shouldCheckParamExists() {
            // Given
            RunnableParams params = RunnableParams.builder()
                    .addParam("exists", "value")
                    .build();

            // Then
            assertTrue(params.hasParam("exists"));
            assertFalse(params.hasParam("notExists"));
        }
    }

    @Nested
    @DisplayName("不可变性测试")
    class ImmutabilityTests {

        @Test
        @DisplayName("customParams 不可修改")
        void shouldNotModifyCustomParams() {
            // Given
            RunnableParams params = RunnableParams.builder()
                    .addParam("key", "value")
                    .build();

            // When & Then
            assertThrows(UnsupportedOperationException.class, () ->
                    params.getCustomParams().put("newKey", "newValue"));
        }

        @Test
        @DisplayName("toolParams 不可修改")
        void shouldNotModifyToolParams() {
            // Given
            RunnableParams params = RunnableParams.builder()
                    .addToolParam("key", "value")
                    .build();

            // When & Then
            assertThrows(UnsupportedOperationException.class, () ->
                    params.getToolParams().put("newKey", "newValue"));
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode 测试")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同内容的参数相等")
        void shouldBeEqualForSameContent() {
            // Given
            RunnableParams params1 = RunnableParams.builder()
                    .conversationId("conv-1")
                    .addParam("key", "value")
                    .build();

            RunnableParams params2 = RunnableParams.builder()
                    .conversationId("conv-1")
                    .addParam("key", "value")
                    .build();

            // Then
            assertEquals(params1, params2);
            assertEquals(params1.hashCode(), params2.hashCode());
        }

        @Test
        @DisplayName("不同内容的参数不相等")
        void shouldNotBeEqualForDifferentContent() {
            // Given
            RunnableParams params1 = RunnableParams.builder()
                    .conversationId("conv-1")
                    .build();

            RunnableParams params2 = RunnableParams.builder()
                    .conversationId("conv-2")
                    .build();

            // Then
            assertNotEquals(params1, params2);
        }
    }

    @Nested
    @DisplayName("customParams 和 toolParams 区别测试")
    class ParamsDifferenceTests {

        @Test
        @DisplayName("customParams 和 toolParams 是独立的")
        void shouldKeepParamsIndependent() {
            // Given & When
            RunnableParams params = RunnableParams.builder()
                    .addParam("customKey", "customValue")
                    .addToolParam("toolKey", "toolValue")
                    .build();

            // Then
            assertTrue(params.hasParam("customKey"));
            assertFalse(params.hasParam("toolKey")); // toolKey 不在 customParams 中
            assertEquals("customValue", params.getParam("customKey"));
            assertEquals("toolValue", params.getToolParams().get("toolKey"));
        }
    }
}
