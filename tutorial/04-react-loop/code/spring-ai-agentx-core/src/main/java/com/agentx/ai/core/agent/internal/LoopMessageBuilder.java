package com.agentx.ai.core.agent.internal;

import com.agentx.ai.core.model.RunnableParams;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 消息构建器 — 构建初始消息列表。
 * <p>
 * 本节新增：customParams 注入到系统提示词中。
 *
 * @author bigchui
 */
public class LoopMessageBuilder {

    private final String instructions;

    public LoopMessageBuilder(String instructions) {
        this.instructions = instructions;
    }

    /**
     * 构建初始消息列表。
     *
     * @param query  用户问题
     * @param params 调用参数（customParams 注入系统提示词）
     * @return 消息列表
     */
    public List<Message> buildMessages(String query, RunnableParams params) {
        List<Message> messages = new ArrayList<>();

        // 1. 系统提示词
        String systemPrompt = (instructions != null && !instructions.isBlank()) ? instructions : "";

        // 注入 customParams（LLM 可见）
        if (params != null && params.getCustomParams() != null && !params.getCustomParams().isEmpty()) {
            StringBuilder sb = new StringBuilder(systemPrompt);
            sb.append("\n\n## 系统参数（LLM 可见）\n");
            for (Map.Entry<String, Object> entry : params.getCustomParams().entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            systemPrompt = sb.toString();
        }

        if (!systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 2. 用户消息
        messages.add(new UserMessage(query));

        return messages;
    }
}
