package com.agentx.ai.core.agent;

import com.agentx.ai.core.agent.internal.AgentLoopExecutor;
import com.agentx.ai.core.agent.internal.LoopMessageBuilder;
import com.agentx.ai.core.agent.internal.SessionPersister;
import com.agentx.ai.core.agent.internal.ToolCallExecutor;
import com.agentx.ai.core.memory.store.ConversationStore;
import com.agentx.ai.core.memory.store.SessionMessageStore;
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

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ReactAgent - 基于 ReAct 范式的智能体实现（会话持久化版本）。
 * <p>
 * 本节新增：dataSource / enableSession 属性，开启后 Agent 自动持久化会话消息到数据库，
 * 下次调用时加载历史上下文，实现多轮对话记忆。
 *
 * @author bigchui
 */
public class ReactAgent {

    private static final Logger log = LoggerFactory.getLogger(ReactAgent.class);

    private final ChatModel chatModel;
    private final String instructions;
    private final int maxRounds;
    private final List<ToolCallback> tools;
    private final DataSource dataSource;
    private final boolean enableSession;

    private ReactAgent(Builder builder) {
        this.chatModel = builder.chatModel;
        this.instructions = builder.instructions;
        this.maxRounds = builder.maxRounds;
        this.tools = List.copyOf(builder.tools);
        this.dataSource = builder.dataSource;
        this.enableSession = builder.enableSession;
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

    public Flux<String> stream(String query) {
        return stream(query, RunnableParams.empty());
    }

    public Flux<String> stream(String query, RunnableParams params) {
        return createExecutor().stream(query, params)
                .filter(e -> e instanceof AgentStreamEvent.Text)
                .map(e -> ((AgentStreamEvent.Text) e).content());
    }

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

        // 构建会话存储
        SessionMessageStore sessionMessageStore = null;
        ConversationStore conversationStore = null;
        SessionPersister sessionPersister = null;

        if (enableSession && dataSource != null) {
            sessionMessageStore = new SessionMessageStore(dataSource);
            conversationStore = new ConversationStore(dataSource);
            sessionPersister = new SessionPersister(true, conversationStore, sessionMessageStore);
        }

        // 构建消息构建器（传入 sessionMessageStore 以支持历史加载）
        LoopMessageBuilder messageBuilder = new LoopMessageBuilder(instructions, sessionMessageStore);

        return new AgentLoopExecutor(
                chatClient,
                messageBuilder,
                new ToolCallExecutor(tools),
                maxRounds,
                sessionPersister);
    }

    // ==================== Getters ====================

    public ChatModel getChatModel() { return chatModel; }
    public String getInstructions() { return instructions; }
    public int getMaxRounds() { return maxRounds; }
    public List<ToolCallback> getTools() { return tools; }
    public boolean isEnableSession() { return enableSession; }

    /**
     * Builder 模式构建 ReactAgent。
     */
    public static class Builder {
        private ChatModel chatModel;
        private String instructions;
        private int maxRounds = 100;
        private final List<ToolCallback> tools = new ArrayList<>();
        private DataSource dataSource;
        private boolean enableSession;

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
        public Builder dataSource(DataSource dataSource) { this.dataSource = dataSource; return this; }
        public Builder enableSession(boolean enableSession) { this.enableSession = enableSession; return this; }

        public ReactAgent build() {
            Objects.requireNonNull(chatModel, "chatModel must not be null");
            return new ReactAgent(this);
        }
    }
}
