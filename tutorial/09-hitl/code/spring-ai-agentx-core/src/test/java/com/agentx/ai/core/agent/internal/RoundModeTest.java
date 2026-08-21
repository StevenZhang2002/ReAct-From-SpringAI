package com.agentx.ai.core.agent.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoundMode 枚举测试。
 */
@DisplayName("RoundMode 测试")
class RoundModeTest {

    @Test
    @DisplayName("RoundMode 有两个值")
    void shouldHaveTwoValues() {
        assertEquals(2, RoundMode.values().length);
    }

    @Test
    @DisplayName("TEXT 模式")
    void shouldHaveTextMode() {
        assertNotNull(RoundMode.TEXT);
    }

    @Test
    @DisplayName("TOOL_CALL 模式")
    void shouldHaveToolCallMode() {
        assertNotNull(RoundMode.TOOL_CALL);
    }

    @Test
    @DisplayName("valueOf 测试")
    void shouldSupportValueOf() {
        assertEquals(RoundMode.TEXT, RoundMode.valueOf("TEXT"));
        assertEquals(RoundMode.TOOL_CALL, RoundMode.valueOf("TOOL_CALL"));
    }
}
