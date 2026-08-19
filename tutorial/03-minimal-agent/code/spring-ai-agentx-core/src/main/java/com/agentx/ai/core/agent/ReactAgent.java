package com.agentx.ai.core.agent;

import com.agentx.ai.core.agent.internal.AgentLoopExecutor;
import com.agentx.ai.core.agent.internal.LoopMessageBuilder;
import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.RunnableParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Objects;

/**
 * ReactAgent - 基于 ReAct 范式的智能体实现（最简版本）。
 * <p>
 * 本节只实现最基本的同步调用：chatModel + instructions → call(query) → answer。
 * <p>
 * 完整版的 ReactAgent 有 20+ 个属性，本节只引入 2 个核心属性：
 * <ul>
 *   <li>{@code chatModel} — LLM 模型</li>
 *   <li>{@code instructions} — 系统提示词</li>
 * </ul>
 * <p>
 * 后续章节逐步添加：
 * <ul>
 *   <li>第 04 节：maxRounds（多轮循环）</li>
 *   <li>第 05 节：tools（工具调用）</li>
 *   <li>第 06 节：stream（流式输出）</li>
 *   <li>第 07 节：sessionMessageStore（会话持久化）</li>
 *   <li>...</li>
 * </ul>
 *
 * @author bigchui
 */
public class ReactAgent {

    private static final Logger log = LoggerFactory.getLogger(ReactAgent.class);

    private final ChatModel chatModel;
    private final String instructions;

    private ReactAgent(Builder builder) {
        this.chatModel = builder.chatModel;
        this.instructions = builder.instructions;
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 同步调用 Agent。
     *
     * @param query 用户消息
     * @return Agent 响应文本
     */
    public String call(String query) {
        return call(query, RunnableParams.empty());
    }

    /**
     * 同步调用 Agent（带参数）。
     *
     * @param query  用户消息
     * @param params 调用参数
     * @return Agent 响应文本
     */
    public String call(String query, RunnableParams params) {
        AgentResult result = createExecutor().call(query, params);
        return result.answer();
    }

    /**
     * 同步调用 Agent，返回完整结果。
     *
     * @param query  用户消息
     * @param params 调用参数
     * @return AgentResult（Completed 或 Failed）
     */
    public AgentResult callForResult(String query, RunnableParams params) {
        return createExecutor().call(query, params);
    }

    /**
     * 每次调用创建新的 executor（线程安全）。
     */
    private AgentLoopExecutor createExecutor() {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        return new AgentLoopExecutor(chatClient, new LoopMessageBuilder(instructions));
    }

    // ==================== Getters ====================

    public ChatModel getChatModel() {
        return chatModel;
    }

    public String getInstructions() {
        return instructions;
    }

    /**
     * Builder 模式构建 ReactAgent。
     */
    public static class Builder {
        private ChatModel chatModel;
        private String instructions;

        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public ReactAgent build() {
            Objects.requireNonNull(chatModel, "chatModel must not be null");
            return new ReactAgent(this);
        }
    }
}
