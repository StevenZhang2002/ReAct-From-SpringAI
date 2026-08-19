package com.agentx.ai.core.agent.internal;

import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.RunnableParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Agent ReAct 循环执行器 — 最简版本。
 * <p>
 * 本节只实现单轮 LLM 调用：接收消息列表 → 调用 LLM → 返回结果。
 * <p>
 * 后续章节逐步添加：
 * <ul>
 *   <li>第 04 节：多轮循环（检测 tool_calls → 执行工具 → 再次调用 LLM）</li>
 *   <li>第 05 节：工具执行</li>
 *   <li>第 06 节：流式输出</li>
 * </ul>
 *
 * @author bigchui
 */
public class AgentLoopExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentLoopExecutor.class);

    private final ChatClient chatClient;
    private final LoopMessageBuilder messageBuilder;

    public AgentLoopExecutor(ChatClient chatClient, LoopMessageBuilder messageBuilder) {
        this.chatClient = chatClient;
        this.messageBuilder = messageBuilder;
    }

    /**
     * 同步调用 LLM。
     *
     * @param query  用户消息
     * @param params 调用参数（本节暂不使用）
     * @return AgentResult
     */
    public AgentResult call(String query, RunnableParams params) {
        // 1. 构建消息
        List<Message> messages = messageBuilder.buildMessages(query);

        // 2. 调用 LLM
        log.debug("Calling LLM with {} messages", messages.size());
        String answer = chatClient.prompt()
                .messages(messages)
                .call()
                .content();

        // 3. 返回结果
        return new AgentResult.Completed(answer);
    }
}
