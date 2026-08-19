package com.agentx.ai.core.agent.internal;

import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.RunnableParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * Agent ReAct 循环执行器 — 多轮循环版本。
 * <p>
 * 本节核心升级：从单轮调用变为多轮 ReAct 循环。
 * <p>
 * 循环逻辑：
 * <ol>
 *   <li>调用 LLM</li>
 *   <li>检查响应是否包含 tool_calls</li>
 *   <li>无 tool_calls → 返回最终答案</li>
 *   <li>有 tool_calls → 执行工具 → 将结果加入消息 → 回到步骤 1</li>
 *   <li>达到 maxRounds → 强制返回</li>
 * </ol>
 *
 * @author bigchui
 */
public class AgentLoopExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentLoopExecutor.class);

    private final ChatClient chatClient;
    private final LoopMessageBuilder messageBuilder;
    private final ToolCallExecutor toolCallExecutor;
    private final int maxRounds;

    public AgentLoopExecutor(ChatClient chatClient, LoopMessageBuilder messageBuilder,
                             ToolCallExecutor toolCallExecutor, int maxRounds) {
        this.chatClient = chatClient;
        this.messageBuilder = messageBuilder;
        this.toolCallExecutor = toolCallExecutor;
        this.maxRounds = maxRounds;
    }

    /**
     * 同步执行 ReAct 循环。
     *
     * @param query  用户消息
     * @param params 调用参数
     * @return AgentResult
     */
    public AgentResult call(String query, RunnableParams params) {
        // 1. 构建初始消息
        List<Message> messages = messageBuilder.buildMessages(query, params);

        // 2. 多轮循环
        for (int round = 1; round <= maxRounds; round++) {
            log.debug("Round {}/{}", round, maxRounds);

            // 调用 LLM
            ChatResponse response = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null) {
                return new AgentResult.Failed("LLM 返回空响应",
                        com.agentx.ai.core.exception.AgentErrorCode.LLM_CALL_FAILED);
            }

            AssistantMessage assistantMsg = response.getResult().getOutput();
            List<AssistantMessage.ToolCall> toolCalls = assistantMsg.getToolCalls();

            // 无工具调用 → 返回最终答案
            if (toolCalls == null || toolCalls.isEmpty()) {
                messages.add(assistantMsg);
                String answer = assistantMsg.getText();
                return new AgentResult.Completed(answer != null ? answer : "");
            }

            // 有工具调用 → 校验参数 → 执行工具 → 添加结果消息
            List<AssistantMessage.ToolCall> safeToolCalls = toolCallExecutor.sanitizeToolCalls(toolCalls);
            messages.add(assistantMsg);

            for (AssistantMessage.ToolCall tc : safeToolCalls) {
                String result = toolCallExecutor.execute(tc, params);
                messages.add(toolCallExecutor.buildToolResponseMessage(tc, result));
            }
            // 继续下一轮
        }

        // 达到最大轮次
        log.warn("Max rounds ({}) reached", maxRounds);
        return new AgentResult.Failed("达到最大轮次限制 (" + maxRounds + ")",
                com.agentx.ai.core.exception.AgentErrorCode.LLM_CALL_FAILED);
    }
}
