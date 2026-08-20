package com.agentx.ai.core.agent;

import com.agentx.ai.core.agent.internal.AgentLoopExecutor;
import com.agentx.ai.core.agent.internal.AgentTaskManager;
import com.agentx.ai.core.agent.internal.LoopMessageBuilder;
import com.agentx.ai.core.agent.internal.SessionPersister;
import com.agentx.ai.core.agent.internal.ThinkingModeProcessor;
import com.agentx.ai.core.agent.internal.ToolCallExecutor;
import com.agentx.ai.core.interrupt.PauseReason;
import com.agentx.ai.core.memory.store.ConversationStore;
import com.agentx.ai.core.memory.store.SessionMessageStore;
import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.AgentStreamEvent;
import com.agentx.ai.core.model.PauseState;
import com.agentx.ai.core.model.PendingToolCall;
import com.agentx.ai.core.model.RunnableParams;
import com.agentx.ai.core.model.ThinkingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ReactAgent - 基于 ReAct 范式的智能体实现（HITL 版本）。
 * <p>
 * 本节新增：askUser 属性 + resume() 方法。
 * 当 askUser=true 时，ask_user 工具调用会被拦截，返回 Paused 结果。
 *
 * @author bigchui
 */
public class ReactAgent {

    private static final Logger log = LoggerFactory.getLogger(ReactAgent.class);
    private static final String ASK_USER_TOOL_NAME = "ask_user";

    private final ChatModel chatModel;
    private final String instructions;
    private final int maxRounds;
    private final List<ToolCallback> tools;
    private final DataSource dataSource;
    private final boolean enableSession;
    private final AgentTaskManager taskManager;
    private final ThinkingMode thinkingMode;
    private final boolean askUser;

    private ReactAgent(Builder builder) {
        this.chatModel = builder.chatModel;
        this.instructions = builder.instructions;
        this.maxRounds = builder.maxRounds;
        this.tools = List.copyOf(builder.tools);
        this.dataSource = builder.dataSource;
        this.enableSession = builder.enableSession;
        this.taskManager = builder.taskManager;
        this.thinkingMode = builder.thinkingMode;
        this.askUser = builder.askUser;
    }

    public static Builder builder() { return new Builder(); }

    // ==================== 同步调用 ====================

    public String call(String query) {
        return call(query, RunnableParams.empty());
    }

    public String call(String query, RunnableParams params) {
        AgentResult result = callForResult(query, params);
        return result.answer();
    }

    public AgentResult callForResult(String query, RunnableParams params) {
        return createExecutor().call(query, params);
    }

    // ==================== 恢复执行 ====================

    /**
     * 从暂停状态恢复执行。
     *
     * @param state   暂停状态快照
     * @param answers 用户回答（key=toolCallId, value=用户回答文本）
     * @return 执行结果
     */
    public AgentResult resume(PauseState state, Map<String, String> answers) {
        List<Message> messages = new ArrayList<>(state.getMessages());

        // 将用户回答作为工具结果注入
        for (PendingToolCall ptc : state.getPendingToolCalls()) {
            String userAnswer = answers.getOrDefault(ptc.id(), "用户未回答");
            ToolResponseMessage.ToolResponse tr = new ToolResponseMessage.ToolResponse(
                    ptc.id(), ptc.name(), userAnswer);
            messages.add(ToolResponseMessage.builder().responses(List.of(tr)).build());
        }

        // 继续执行 ReAct 循环
        return createExecutor().resumeFromMessages(messages, state.getParams(),
                state.getCurrentRound());
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

        SessionMessageStore sessionMessageStore = null;
        ConversationStore conversationStore = null;
        SessionPersister sessionPersister = null;

        if (enableSession && dataSource != null) {
            sessionMessageStore = new SessionMessageStore(dataSource);
            conversationStore = new ConversationStore(dataSource);
            sessionPersister = new SessionPersister(true, conversationStore, sessionMessageStore);
        }

        LoopMessageBuilder messageBuilder = new LoopMessageBuilder(instructions, sessionMessageStore);
        ThinkingModeProcessor thinkingProcessor = new ThinkingModeProcessor(thinkingMode);

        return new AgentLoopExecutor(
                chatClient,
                messageBuilder,
                new ToolCallExecutor(tools),
                maxRounds,
                sessionPersister,
                taskManager,
                thinkingProcessor,
                askUser ? ASK_USER_TOOL_NAME : null);
    }

    // ==================== Getters ====================

    public ChatModel getChatModel() { return chatModel; }
    public String getInstructions() { return instructions; }
    public int getMaxRounds() { return maxRounds; }
    public List<ToolCallback> getTools() { return tools; }
    public boolean isEnableSession() { return enableSession; }
    public AgentTaskManager getTaskManager() { return taskManager; }
    public ThinkingMode getThinkingMode() { return thinkingMode; }
    public boolean isAskUser() { return askUser; }

    public static class Builder {
        private ChatModel chatModel;
        private String instructions;
        private int maxRounds = 100;
        private final List<ToolCallback> tools = new ArrayList<>();
        private DataSource dataSource;
        private boolean enableSession;
        private AgentTaskManager taskManager;
        private ThinkingMode thinkingMode = ThinkingMode.DISABLED;
        private boolean askUser;

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
        public Builder taskManager(AgentTaskManager taskManager) { this.taskManager = taskManager; return this; }
        public Builder thinkingMode(ThinkingMode thinkingMode) { this.thinkingMode = thinkingMode; return this; }
        public Builder askUser(boolean askUser) { this.askUser = askUser; return this; }

        public ReactAgent build() {
            Objects.requireNonNull(chatModel, "chatModel must not be null");
            return new ReactAgent(this);
        }
    }
}
