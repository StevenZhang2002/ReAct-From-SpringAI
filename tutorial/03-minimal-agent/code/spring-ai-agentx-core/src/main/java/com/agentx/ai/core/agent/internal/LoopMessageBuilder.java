package com.agentx.ai.core.agent.internal;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息构建器 — 构建初始消息列表。
 * <p>
 * 最简版本：只处理 instructions（系统提示词）和 query（用户消息）。
 * 后续章节逐步添加：历史消息加载、长期记忆注入、自定义参数注入等。
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
     * @param query 用户问题
     * @return 消息列表 [SystemMessage?, UserMessage]
     */
    public List<Message> buildMessages(String query) {
        List<Message> messages = new ArrayList<>();

        // 1. 系统提示词
        if (instructions != null && !instructions.isBlank()) {
            messages.add(new SystemMessage(instructions));
        }

        // 2. 用户消息
        messages.add(new UserMessage(query));

        return messages;
    }
}
