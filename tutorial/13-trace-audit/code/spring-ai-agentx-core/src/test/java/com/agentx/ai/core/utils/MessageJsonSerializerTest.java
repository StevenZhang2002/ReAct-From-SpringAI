package com.agentx.ai.core.utils;

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
 * MessageJsonSerializer 单元测试（第06节）。
 *
 * <p>测试消息序列化/反序列化、OpenAI 兼容 JSON 格式。
 */
@DisplayName("MessageJsonSerializer 测试")
class MessageJsonSerializerTest {

    @Nested
    @DisplayName("序列化测试 (toJson)")
    class ToJsonTests {

        @Test
        @DisplayName("序列化空列表")
        void shouldSerializeEmptyList() {
            // When
            String json = MessageJsonSerializer.toJson(List.of());

            // Then
            assertEquals("[]", json);
        }

        @Test
        @DisplayName("序列化 null 列表")
        void shouldSerializeNullList() {
            // When
            String json = MessageJsonSerializer.toJson(null);

            // Then
            assertEquals("[]", json);
        }

        @Test
        @DisplayName("序列化 UserMessage")
        void shouldSerializeUserMessage() {
            // Given
            List<Message> messages = List.of(new UserMessage("你好"));

            // When
            String json = MessageJsonSerializer.toJson(messages);

            // Then
            assertTrue(json.contains("\"role\":\"user\""));
            assertTrue(json.contains("\"content\":\"你好\""));
        }

        @Test
        @DisplayName("序列化 SystemMessage")
        void shouldSerializeSystemMessage() {
            // Given
            List<Message> messages = List.of(new SystemMessage("你是一个助手"));

            // When
            String json = MessageJsonSerializer.toJson(messages);

            // Then
            assertTrue(json.contains("\"role\":\"system\""));
            assertTrue(json.contains("\"content\":\"你是一个助手\""));
        }

        @Test
        @DisplayName("序列化 AssistantMessage")
        void shouldSerializeAssistantMessage() {
            // Given
            AssistantMessage message = AssistantMessage.builder()
                    .content("这是回答")
                    .build();

            // When
            String json = MessageJsonSerializer.toJson(List.of(message));

            // Then
            assertTrue(json.contains("\"role\":\"assistant\""));
            assertTrue(json.contains("\"content\":\"这是回答\""));
        }

        @Test
        @DisplayName("序列化带工具调用的 AssistantMessage")
        void shouldSerializeAssistantMessageWithToolCalls() {
            // Given
            AssistantMessage.ToolCall toolCall = AssistantMessage.ToolCall.builder()
                    .id("call_123")
                    .name("getWeather")
                    .arguments("{\"city\": \"北京\"}")
                    .build();

            AssistantMessage message = AssistantMessage.builder()
                    .content("")
                    .toolCalls(List.of(toolCall))
                    .build();

            // When
            String json = MessageJsonSerializer.toJson(List.of(message));

            // Then
            assertTrue(json.contains("\"tool_calls\""));
            assertTrue(json.contains("\"name\":\"getWeather\""));
            assertTrue(json.contains("\"id\":\"call_123\""));
        }

        @Test
        @DisplayName("序列化多条消息")
        void shouldSerializeMultipleMessages() {
            // Given
            List<Message> messages = List.of(
                    new SystemMessage("你是助手"),
                    new UserMessage("你好"),
                    AssistantMessage.builder().content("你好！").build()
            );

            // When
            String json = MessageJsonSerializer.toJson(messages);

            // Then
            assertTrue(json.contains("\"role\":\"system\""));
            assertTrue(json.contains("\"role\":\"user\""));
            assertTrue(json.contains("\"role\":\"assistant\""));
        }
    }

    @Nested
    @DisplayName("反序列化测试 (fromJson)")
    class FromJsonTests {

        @Test
        @DisplayName("反序列化空 JSON")
        void shouldDeserializeEmptyJson() {
            // When
            List<Message> messages = MessageJsonSerializer.fromJson("[]");

            // Then
            assertTrue(messages.isEmpty());
        }

        @Test
        @DisplayName("反序列化 null JSON")
        void shouldDeserializeNullJson() {
            // When
            List<Message> messages = MessageJsonSerializer.fromJson(null);

            // Then
            assertTrue(messages.isEmpty());
        }

        @Test
        @DisplayName("反序列化 UserMessage")
        void shouldDeserializeUserMessage() {
            // Given
            String json = "[{\"role\":\"user\",\"content\":\"你好\"}]";

            // When
            List<Message> messages = MessageJsonSerializer.fromJson(json);

            // Then
            assertEquals(1, messages.size());
            assertInstanceOf(UserMessage.class, messages.get(0));
            assertEquals("你好", messages.get(0).getText());
        }

        @Test
        @DisplayName("反序列化 SystemMessage")
        void shouldDeserializeSystemMessage() {
            // Given
            String json = "[{\"role\":\"system\",\"content\":\"你是助手\"}]";

            // When
            List<Message> messages = MessageJsonSerializer.fromJson(json);

            // Then
            assertEquals(1, messages.size());
            assertInstanceOf(SystemMessage.class, messages.get(0));
            assertEquals("你是助手", messages.get(0).getText());
        }

        @Test
        @DisplayName("反序列化 AssistantMessage")
        void shouldDeserializeAssistantMessage() {
            // Given
            String json = "[{\"role\":\"assistant\",\"content\":\"这是回答\"}]";

            // When
            List<Message> messages = MessageJsonSerializer.fromJson(json);

            // Then
            assertEquals(1, messages.size());
            assertInstanceOf(AssistantMessage.class, messages.get(0));
            assertEquals("这是回答", messages.get(0).getText());
        }

        @Test
        @DisplayName("反序列化带工具调用的 AssistantMessage")
        void shouldDeserializeAssistantMessageWithToolCalls() {
            // Given
            String json = """
                    [{
                        "role": "assistant",
                        "content": "",
                        "tool_calls": [{
                            "id": "call_123",
                            "type": "function",
                            "function": {
                                "name": "getWeather",
                                "arguments": "{\\"city\\": \\"北京\\"}"
                            }
                        }]
                    }]
                    """;

            // When
            List<Message> messages = MessageJsonSerializer.fromJson(json);

            // Then
            assertEquals(1, messages.size());
            AssistantMessage assistantMessage = (AssistantMessage) messages.get(0);
            assertNotNull(assistantMessage.getToolCalls());
            assertEquals(1, assistantMessage.getToolCalls().size());
            assertEquals("getWeather", assistantMessage.getToolCalls().get(0).name());
        }
    }

    @Nested
    @DisplayName("往返测试")
    class RoundTripTests {

        @Test
        @DisplayName("UserMessage 往返")
        void shouldRoundTripUserMessage() {
            // Given
            List<Message> original = List.of(new UserMessage("测试消息"));

            // When
            String json = MessageJsonSerializer.toJson(original);
            List<Message> deserialized = MessageJsonSerializer.fromJson(json);

            // Then
            assertEquals(1, deserialized.size());
            assertEquals("测试消息", deserialized.get(0).getText());
        }

        @Test
        @DisplayName("多条消息往返")
        void shouldRoundTripMultipleMessages() {
            // Given
            List<Message> original = List.of(
                    new SystemMessage("系统消息"),
                    new UserMessage("用户消息"),
                    AssistantMessage.builder().content("助手回复").build()
            );

            // When
            String json = MessageJsonSerializer.toJson(original);
            List<Message> deserialized = MessageJsonSerializer.fromJson(json);

            // Then
            assertEquals(3, deserialized.size());
            assertInstanceOf(SystemMessage.class, deserialized.get(0));
            assertInstanceOf(UserMessage.class, deserialized.get(1));
            assertInstanceOf(AssistantMessage.class, deserialized.get(2));
        }
    }

    @Nested
    @DisplayName("toMaps 测试")
    class ToMapsTests {

        @Test
        @DisplayName("转换为 Map 列表")
        void shouldConvertToMaps() {
            // Given
            List<Message> messages = List.of(new UserMessage("你好"));

            // When
            var maps = MessageJsonSerializer.toMaps(messages);

            // Then
            assertEquals(1, maps.size());
            assertEquals("user", maps.get(0).get("role"));
            assertEquals("你好", maps.get(0).get("content"));
        }

        @Test
        @DisplayName("空列表转换为空 Map 列表")
        void shouldConvertEmptyListToEmptyMaps() {
            // When
            var maps = MessageJsonSerializer.toMaps(List.of());

            // Then
            assertTrue(maps.isEmpty());
        }
    }
}
