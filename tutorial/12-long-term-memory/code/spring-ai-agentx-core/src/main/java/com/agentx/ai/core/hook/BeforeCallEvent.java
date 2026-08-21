package com.agentx.ai.core.hook;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * LLM 调用前事件。
 * <p>
 * 可修改：messages 列表可追加/修改（如注入额外上下文）。
 *
 * @author bigchui
 */
public final class BeforeCallEvent implements HookEvent {

    private final String query;
    private List<Message> messages;

    public BeforeCallEvent(String query, List<Message> messages) {
        this.query = query;
        this.messages = messages;
    }

    public String getQuery() {
        return query;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
