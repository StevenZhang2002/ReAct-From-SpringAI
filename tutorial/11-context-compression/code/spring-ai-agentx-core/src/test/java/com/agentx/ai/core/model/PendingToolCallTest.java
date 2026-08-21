package com.agentx.ai.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PendingToolCall 单元测试。
 */
@DisplayName("PendingToolCall 测试")
class PendingToolCallTest {

    @Test
    @DisplayName("创建 PendingToolCall")
    void shouldCreatePendingToolCall() {
        // Given
        String id = "call_123";
        String name = "getWeather";
        String arguments = "{\"city\": \"北京\"}";

        // When
        PendingToolCall toolCall = new PendingToolCall(id, name, arguments);

        // Then
        assertEquals(id, toolCall.id());
        assertEquals(name, toolCall.name());
        assertEquals(arguments, toolCall.arguments());
    }

    @Test
    @DisplayName("record 相等性测试")
    void shouldBeEqualForSameValues() {
        // Given
        PendingToolCall call1 = new PendingToolCall("id1", "tool1", "{}");
        PendingToolCall call2 = new PendingToolCall("id1", "tool1", "{}");

        // Then
        assertEquals(call1, call2);
        assertEquals(call1.hashCode(), call2.hashCode());
    }

    @Test
    @DisplayName("record 不相等测试")
    void shouldNotBeEqualForDifferentValues() {
        // Given
        PendingToolCall call1 = new PendingToolCall("id1", "tool1", "{}");
        PendingToolCall call2 = new PendingToolCall("id2", "tool1", "{}");

        // Then
        assertNotEquals(call1, call2);
    }
}
