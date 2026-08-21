package com.agentx.ai.core.agent;

import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.RunnableParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ReactAgent 单元测试（第03节）。
 *
 * <p>测试 Builder 模式、基本调用、结果获取。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReactAgent 测试")
class ReactAgentTest {

    @Mock
    private ChatModel chatModel;

    @Nested
    @DisplayName("Builder 模式测试")
    class BuilderTests {

        @Test
        @DisplayName("使用 Builder 创建 ReactAgent")
        void shouldBuildReactAgent() {
            // Given
            String instructions = "你是一个助手";

            // When
            ReactAgent agent = ReactAgent.builder()
                    .chatModel(chatModel)
                    .instructions(instructions)
                    .build();

            // Then
            assertNotNull(agent);
            assertEquals(chatModel, agent.getChatModel());
            assertEquals(instructions, agent.getInstructions());
        }

        @Test
        @DisplayName("chatModel 为 null 时抛出异常")
        void shouldThrowWhenChatModelIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                    ReactAgent.builder()
                            .instructions("测试")
                            .build());
        }

        @Test
        @DisplayName("instructions 可以为 null")
        void shouldAllowNullInstructions() {
            // When
            ReactAgent agent = ReactAgent.builder()
                    .chatModel(chatModel)
                    .build();

            // Then
            assertNotNull(agent);
            assertNull(agent.getInstructions());
        }
    }

    @Nested
    @DisplayName("call() 方法测试")
    class CallTests {

        @Test
        @DisplayName("call() 返回 LLM 响应")
        void shouldReturnLlmResponse() {
            // Given
            String expectedAnswer = "你好！我是 AI 助手。";
            mockChatModelResponse(expectedAnswer);

            ReactAgent agent = ReactAgent.builder()
                    .chatModel(chatModel)
                    .instructions("你是一个助手")
                    .build();

            // When
            String result = agent.call("你好");

            // Then
            assertEquals(expectedAnswer, result);
            verify(chatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("call() 带 RunnableParams")
        void shouldCallWithParams() {
            // Given
            String expectedAnswer = "收到参数";
            mockChatModelResponse(expectedAnswer);

            ReactAgent agent = ReactAgent.builder()
                    .chatModel(chatModel)
                    .instructions("你是一个助手")
                    .build();

            RunnableParams params = RunnableParams.builder()
                    .conversationId("conv-123")
                    .addParam("language", "zh-CN")
                    .build();

            // When
            String result = agent.call("测试", params);

            // Then
            assertEquals(expectedAnswer, result);
        }
    }

    @Nested
    @DisplayName("callForResult() 方法测试")
    class CallForResultTests {

        @Test
        @DisplayName("callForResult() 返回 Completed 结果")
        void shouldReturnCompletedResult() {
            // Given
            String expectedAnswer = "这是答案";
            mockChatModelResponse(expectedAnswer);

            ReactAgent agent = ReactAgent.builder()
                    .chatModel(chatModel)
                    .instructions("你是一个助手")
                    .build();

            // When
            AgentResult result = agent.callForResult("问题", RunnableParams.empty());

            // Then
            assertInstanceOf(AgentResult.Completed.class, result);
            assertFalse(result.isFailed());
            assertEquals(expectedAnswer, result.answer());
        }

        @Test
        @DisplayName("callForResult() 结果可以获取答案")
        void shouldGetAnswerFromResult() {
            // Given
            String expectedAnswer = "测试答案";
            mockChatModelResponse(expectedAnswer);

            ReactAgent agent = ReactAgent.builder()
                    .chatModel(chatModel)
                    .build();

            // When
            AgentResult result = agent.callForResult("问题", RunnableParams.empty());

            // Then
            assertEquals(expectedAnswer, result.answer());
        }
    }

    @Nested
    @DisplayName("Getters 测试")
    class GetterTests {

        @Test
        @DisplayName("获取 chatModel")
        void shouldGetChatModel() {
            // Given
            ReactAgent agent = ReactAgent.builder()
                    .chatModel(chatModel)
                    .build();

            // Then
            assertEquals(chatModel, agent.getChatModel());
        }

        @Test
        @DisplayName("获取 instructions")
        void shouldGetInstructions() {
            // Given
            String instructions = "你是一个专业的助手";
            ReactAgent agent = ReactAgent.builder()
                    .chatModel(chatModel)
                    .instructions(instructions)
                    .build();

            // Then
            assertEquals(instructions, agent.getInstructions());
        }
    }

    /**
     * 辅助方法：模拟 ChatModel 返回响应
     */
    private void mockChatModelResponse(String content) {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(
                org.springframework.ai.chat.messages.AssistantMessage.builder()
                        .text(content)
                        .build()
        );
    }
}
