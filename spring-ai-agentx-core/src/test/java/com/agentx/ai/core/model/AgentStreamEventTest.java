package com.agentx.ai.core.model;

import com.agentx.ai.core.exception.AgentErrorCode;
import com.agentx.ai.core.exception.AgentException;
import com.agentx.ai.core.interrupt.SafePoint;
import com.agentx.ai.core.interrupt.PauseReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentStreamEvent 单元测试。
 *
 * <p>测试 sealed 接口的各种事件类型创建和便捷构造。
 */
@DisplayName("AgentStreamEvent 测试")
class AgentStreamEventTest {

    @Nested
    @DisplayName("Thinking 事件")
    class ThinkingTests {

        @Test
        @DisplayName("创建 Thinking 事件 - 便捷构造")
        void shouldCreateThinkingWithContent() {
            AgentStreamEvent event = new AgentStreamEvent.Thinking("思考内容");
            assertInstanceOf(AgentStreamEvent.Thinking.class, event);
            assertEquals("思考内容", ((AgentStreamEvent.Thinking) event).content());
            assertNull(((AgentStreamEvent.Thinking) event).source());
        }

        @Test
        @DisplayName("创建 Thinking 事件 - 带 source")
        void shouldCreateThinkingWithSource() {
            AgentStreamEvent event = new AgentStreamEvent.Thinking("思考", null);
            assertInstanceOf(AgentStreamEvent.Thinking.class, event);
        }
    }

    @Nested
    @DisplayName("Text 事件")
    class TextTests {

        @Test
        @DisplayName("创建 Text 事件 - 便捷构造")
        void shouldCreateTextWithContent() {
            AgentStreamEvent event = new AgentStreamEvent.Text("回答内容");
            assertInstanceOf(AgentStreamEvent.Text.class, event);
            assertEquals("回答内容", ((AgentStreamEvent.Text) event).content());
            assertNull(((AgentStreamEvent.Text) event).source());
        }
    }

    @Nested
    @DisplayName("ToolStart 事件")
    class ToolStartTests {

        @Test
        @DisplayName("创建 ToolStart 事件")
        void shouldCreateToolStart() {
            AgentStreamEvent event = new AgentStreamEvent.ToolStart("getWeather", "call_1", "{\"city\":\"北京\"}");
            assertInstanceOf(AgentStreamEvent.ToolStart.class, event);
            AgentStreamEvent.ToolStart ts = (AgentStreamEvent.ToolStart) event;
            assertEquals("getWeather", ts.toolName());
            assertEquals("call_1", ts.toolCallId());
            assertEquals("{\"city\":\"北京\"}", ts.arguments());
            assertNull(ts.source());
        }
    }

    @Nested
    @DisplayName("ToolEnd 事件")
    class ToolEndTests {

        @Test
        @DisplayName("创建 ToolEnd 事件")
        void shouldCreateToolEnd() {
            AgentStreamEvent event = new AgentStreamEvent.ToolEnd("getWeather", "call_1", "晴天");
            assertInstanceOf(AgentStreamEvent.ToolEnd.class, event);
            AgentStreamEvent.ToolEnd te = (AgentStreamEvent.ToolEnd) event;
            assertEquals("getWeather", te.toolName());
            assertEquals("晴天", te.result());
        }
    }

    @Nested
    @DisplayName("Error 事件")
    class ErrorTests {

        @Test
        @DisplayName("创建 Error 事件")
        void shouldCreateError() {
            AgentStreamEvent event = new AgentStreamEvent.Error(
                    AgentErrorCode.LLM_CALL_FAILED, "调用失败", "详细错误");
            assertInstanceOf(AgentStreamEvent.Error.class, event);
            AgentStreamEvent.Error err = (AgentStreamEvent.Error) event;
            assertEquals(AgentErrorCode.LLM_CALL_FAILED, err.code());
            assertEquals("调用失败", err.message());
            assertEquals("详细错误", err.detail());
        }
    }

    @Nested
    @DisplayName("Complete 事件")
    class CompleteTests {

        @Test
        @DisplayName("创建 Complete 事件 - 默认构造")
        void shouldCreateCompleteDefault() {
            AgentStreamEvent event = new AgentStreamEvent.Complete();
            assertInstanceOf(AgentStreamEvent.Complete.class, event);
            AgentStreamEvent.Complete c = (AgentStreamEvent.Complete) event;
            assertEquals(0, c.totalPromptTokens());
            assertEquals(0, c.totalCompletionTokens());
            assertNull(c.conversationId());
            assertNull(c.sessionId());
        }

        @Test
        @DisplayName("创建 Complete 事件 - 带 tokens")
        void shouldCreateCompleteWithTokens() {
            AgentStreamEvent event = new AgentStreamEvent.Complete(100, 50);
            AgentStreamEvent.Complete c = (AgentStreamEvent.Complete) event;
            assertEquals(100, c.totalPromptTokens());
            assertEquals(50, c.totalCompletionTokens());
        }

        @Test
        @DisplayName("创建 Complete 事件 - 完整构造")
        void shouldCreateCompleteFull() {
            AgentStreamEvent event = new AgentStreamEvent.Complete(100, 50, "conv-1", 12345L, null);
            AgentStreamEvent.Complete c = (AgentStreamEvent.Complete) event;
            assertEquals("conv-1", c.conversationId());
            assertEquals(12345L, c.sessionId());
        }
    }

    @Nested
    @DisplayName("Paused 事件")
    class PausedTests {

        @Test
        @DisplayName("创建 Paused 事件")
        void shouldCreatePaused() {
            PauseState state = PauseState.builder()
                    .reason(PauseReason.HITL_TOOL_REQUEST)
                    .build();
            AgentStreamEvent event = new AgentStreamEvent.Paused(state);
            assertInstanceOf(AgentStreamEvent.Paused.class, event);
            assertEquals(state, ((AgentStreamEvent.Paused) event).state());
        }
    }

    @Nested
    @DisplayName("sealed 接口模式匹配")
    class PatternMatchingTests {

        @Test
        @DisplayName("switch 模式匹配 - Text")
        void shouldMatchText() {
            AgentStreamEvent event = new AgentStreamEvent.Text("内容");
            String result = switch (event) {
                case AgentStreamEvent.Text t -> "text:" + t.content();
                case AgentStreamEvent.Thinking t -> "thinking";
                case AgentStreamEvent.ToolStart ts -> "toolStart";
                case AgentStreamEvent.ToolEnd te -> "toolEnd";
                case AgentStreamEvent.Paused p -> "paused";
                case AgentStreamEvent.Error e -> "error";
                case AgentStreamEvent.Complete c -> "complete";
            };
            assertEquals("text:内容", result);
        }

        @Test
        @DisplayName("switch 模式匹配 - Complete")
        void shouldMatchComplete() {
            AgentStreamEvent event = new AgentStreamEvent.Complete(100, 50);
            String result = switch (event) {
                case AgentStreamEvent.Complete c -> "tokens:" + c.totalPromptTokens();
                default -> "other";
            };
            assertEquals("tokens:100", result);
        }
    }
}
