package com.agentx.ai.core.interrupt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PauseReason 枚举测试。
 */
@DisplayName("PauseReason 测试")
class PauseReasonTest {

    @Test
    @DisplayName("PauseReason 有两个值")
    void shouldHaveTwoValues() {
        assertEquals(2, PauseReason.values().length);
    }

    @Test
    @DisplayName("HITL_TOOL_REQUEST 值")
    void shouldHaveHitlToolRequest() {
        assertNotNull(PauseReason.HITL_TOOL_REQUEST);
    }

    @Test
    @DisplayName("USER_INTERRUPT 值")
    void shouldHaveUserInterrupt() {
        assertNotNull(PauseReason.USER_INTERRUPT);
    }

    @Test
    @DisplayName("valueOf 测试")
    void shouldSupportValueOf() {
        assertEquals(PauseReason.HITL_TOOL_REQUEST, PauseReason.valueOf("HITL_TOOL_REQUEST"));
        assertEquals(PauseReason.USER_INTERRUPT, PauseReason.valueOf("USER_INTERRUPT"));
    }
}
