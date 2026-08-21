package com.agentx.ai.core.agent.internal;

import com.agentx.ai.core.memory.store.SessionMessageStore;
import com.agentx.ai.core.model.RunnableParams;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 消息构建器 — 构建初始消息列表。
 * <p>
 * 本节新增：支持从 SessionMessageStore 加载历史消息，实现多轮对话记忆。
 * <p>
 * 消息构建顺序：
 * <ol>
 *   <li>SystemMessage（系统提示词 + customParams）</li>
 *   <li>历史消息（从 DB 加载的 working_messages）← 本节新增</li>
 *   <li>UserMessage（本次用户提问）</li>
 * </ol>
 *
 * @author bigchui
 */
public class LoopMessageBuilder {

    private final String instructions;

    @Nullable
    private final SessionMessageStore sessionMessageStore;

    public LoopMessageBuilder(String instructions) {
        this(instructions, null);
    }

    public LoopMessageBuilder(String instructions,
                              @Nullable SessionMessageStore sessionMessageStore) {
        this.instructions = instructions;
        this.sessionMessageStore = sessionMessageStore;
    }

    /**
     * 构建初始消息列表。
     *
     * @param query  用户问题
     * @param params 调用参数（conversationId 用于加载历史，customParams 注入系统提示词）
     * @return 消息列表
     */
    public List<Message> buildMessages(String query, RunnableParams params) {
        List<Message> messages = new ArrayList<>();

        // 1. 系统提示词
        String systemPrompt = buildSystemPrompt(params);
        if (!systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 2. 历史消息（从 DB 加载 working_messages）
        loadHistory(messages, params);

        // 3. 用户消息
        messages.add(new UserMessage(query));

        return messages;
    }

    /**
     * 构建系统提示词（含 customParams 注入）。
     */
    private String buildSystemPrompt(RunnableParams params) {
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

        return systemPrompt;
    }

    /**
     * 从 SessionMessageStore 加载历史消息。
     * 仅当 enableSession=true 且 conversationId 不为空时生效。
     */
    private void loadHistory(List<Message> messages, RunnableParams params) {
        if (sessionMessageStore == null || params == null) {
            return;
        }
        String conversationId = params.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }

        List<Message> history = sessionMessageStore.getMessages(conversationId, "working_messages");
        if (!history.isEmpty()) {
            messages.addAll(history);
        }
    }
}
