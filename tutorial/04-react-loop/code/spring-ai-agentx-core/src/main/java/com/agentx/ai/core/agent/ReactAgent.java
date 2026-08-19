package com.agentx.ai.core.agent;

import com.agentx.ai.core.agent.internal.AgentLoopExecutor;
import com.agentx.ai.core.agent.internal.LoopMessageBuilder;
import com.agentx.ai.core.agent.internal.ToolCallExecutor;
import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.RunnableParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ReactAgent - 基于 ReAct 范式的智能体实现（多轮循环版本）。
 * <p>
 * 本节新增属性：
 * <ul>
 *   <li>{@code maxRounds} — 最大循环轮次（默认 100）</li>
 *   <li>{@code tools} — 工具列表</li>
 * </ul>
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

    private AgentLoopExecutor createExecutor() {
        // 构建 ChatClient，配置工具（internalToolExecutionEnabled=false）
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

    public ChatModel getChatModel() {
        return chatModel;
    }

    public String getInstructions() {
        return instructions;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public List<ToolCallback> getTools() {
        return tools;
    }

    /**
     * Builder 模式构建 ReactAgent。
     */
    public static class Builder {
        private ChatModel chatModel;
        private String instructions;
        private int maxRounds = 100;
        private final List<ToolCallback> tools = new ArrayList<>();

        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public Builder maxRounds(int maxRounds) {
            this.maxRounds = maxRounds;
            return this;
        }

        public Builder tools(ToolCallback... tools) {
            if (tools != null) {
                for (ToolCallback tool : tools) {
                    this.tools.add(tool);
                }
            }
            return this;
        }

        public Builder tools(List<ToolCallback> tools) {
            if (tools != null) {
                this.tools.addAll(tools);
            }
            return this;
        }

        public ReactAgent build() {
            Objects.requireNonNull(chatModel, "chatModel must not be null");
            return new ReactAgent(this);
        }
    }
}
