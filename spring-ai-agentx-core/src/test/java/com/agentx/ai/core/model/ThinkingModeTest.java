package com.agentx.ai.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ThinkingMode 枚举测试。
 */
@DisplayName("ThinkingMode 测试")
class ThinkingModeTest {

    @Test
    @DisplayName("ThinkingMode 有三个值")
    void shouldHaveThreeValues() {
        assertEquals(3, ThinkingMode.values().length);
    }

    @Test
    @DisplayName("DISABLED 值")
    void shouldHaveDisabled() {
        assertNotNull(ThinkingMode.DISABLED);
    }

    @Test
    @DisplayName("THINK_TAG 值")
    void shouldHaveThinkTag() {
        assertNotNull(ThinkingMode.THINK_TAG);
    }

    @Test
    @DisplayName("REASONING_CONTENT 值")
    void shouldHaveReasoningContent() {
        assertNotNull(ThinkingMode.REASONING_CONTENT);
    }

    @Test
    @DisplayName("valueOf 测试")
    void shouldSupportValueOf() {
        assertEquals(ThinkingMode.DISABLED, ThinkingMode.valueOf("DISABLED"));
        assertEquals(ThinkingMode.THINK_TAG, ThinkingMode.valueOf("THINK_TAG"));
        assertEquals(ThinkingMode.REASONING_CONTENT, ThinkingMode.valueOf("REASONING_CONTENT"));
    }
}
