package com.agentx.ai.core.agent;

import com.agentx.ai.core.agent.internal.AgentLoopExecutor;
import com.agentx.ai.core.agent.internal.LoopMessageBuilder;
import com.agentx.ai.core.agent.internal.ToolCallExecutor;
import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.AgentStreamEvent;
import com.agentx.ai.core.model.RunnableParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ReactAgent - 基于 ReAct 范式的智能体实现（流式版本）。
 * <p>
 * 本节新增：stream() 方法，返回 Flux 文本流。
 *
 * @author bigchui
 */
public class ReactAgent {

    private static final Logger log = LoggerFactory.getLogger(ReactAgent.class);

    private final ChatModel chatModel;
    private final String instructions;
    private final int maxRounds;
    private final List<ToolCallback> tools;

    private ReactAgent(Builder builder) {
        this.chatModel = builder.chatModel;
        this.instructions = builder.instructions;
        this.maxRounds = builder.maxRounds;
        this.tools = List.copyOf(builder.tools);
    }

    public static Builder builder() {
        return new Builder();
    }

    // ==================== 同步调用 ====================

    public String call(String query) {
        return call(query, RunnableParams.empty());
    }

    public String call(String query, RunnableParams params) {
        AgentResult result = createExecutor().call(query, params);
        return result.answer();
    }

    public AgentResult callForResult(String query, RunnableParams params) {
        return createExecutor().call(query, params);
    }

    // ==================== 流式调用 ====================

    /**
     * 流式调用 Agent，返回文本流。
     */
    public Flux<String> stream(String query) {
        return stream(query, RunnableParams.empty());
    }

    /**
     * 流式调用 Agent（带参数），返回文本流。
     */
    public Flux<String> stream(String query, RunnableParams params) {
        return createExecutor().stream(query, params)
                .filter(e -> e instanceof AgentStreamEvent.Text)
                .map(e -> ((AgentStreamEvent.Text) e).content());
    }

    /**
     * 流式调用 Agent，返回完整事件流。
     */
    public Flux<AgentStreamEvent> streamForResult(String query, RunnableParams params) {
        return createExecutor().stream(query, params);
    }

    // ==================== 内部 ====================

    private AgentLoopExecutor createExecutor() {
        ChatClient.Builder clientBuilder = ChatClient.builder(chatModel);

        var toolOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(tools.toArray(new ToolCallback[0]))
                .internalToolExecutionEnabled(false)
                .build();
        clientBuilder.defaultOptions(toolOptions);
        clientBuilder.defaultToolCallbacks(tools.toArray(new ToolCallback[0]));

        ChatClient chatClient = clientBuilder.build();

        return new AgentLoopExecutor(
                chatClient,
                new LoopMessageBuilder(instructions),
                new ToolCallExecutor(tools),
                maxRounds);
    }

    // ==================== Getters ====================

    public ChatModel getChatModel() { return chatModel; }
    public String getInstructions() { return instructions; }
    public int getMaxRounds() { return maxRounds; }
    public List<ToolCallback> getTools() { return tools; }

    /**
     * Builder 模式构建 ReactAgent。
     */
    public static class Builder {
        private ChatModel chatModel;
        private String instructions;
        private int maxRounds = 100;
        private final List<ToolCallback> tools = new ArrayList<>();

        public Builder chatModel(ChatModel chatModel) { this.chatModel = chatModel; return this; }
        public Builder instructions(String instructions) { this.instructions = instructions; return this; }
        public Builder maxRounds(int maxRounds) { this.maxRounds = maxRounds; return this; }
        public Builder tools(ToolCallback... tools) {
            if (tools != null) { for (ToolCallback t : tools) this.tools.add(t); }
            return this;
        }
        public Builder tools(List<ToolCallback> tools) {
            if (tools != null) { this.tools.addAll(tools); }
            return this;
        }
        public ReactAgent build() {
            Objects.requireNonNull(chatModel, "chatModel must not be null");
            return new ReactAgent(this);
        }
    }
}
