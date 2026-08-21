package com.agentx.ai.core.agent.internal;

import com.agentx.ai.core.memory.store.ConversationStore;
import com.agentx.ai.core.memory.store.SessionMessageStore;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.SignalType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 会话持久化入口 — 集中处理所有落库副作用。
 * <p>
 * 本节简化版：仅处理会话消息和会话窗口状态。
 * 后续章节会扩展：TraceManager（第 13 节）、PauseState（第 09 节）、MemoryPersistor（第 12 节）。
 * <p>
 * AgentLoopExecutor 在 init 和终态分支中委托本类完成 DB 写入。
 *
 * @author bigchui
 */
public class SessionPersister {

    private static final Logger log = LoggerFactory.getLogger(SessionPersister.class);

    private final boolean enableSession;
    private final ConversationStore conversationStore;
    private final SessionMessageStore sessionMessageStore;

    /** 当前 sessionId（由 initSession 生成） */
    private long sessionId;

    /** CAS 标记：保证终态落库只执行一次 */
    private final AtomicBoolean persisted = new AtomicBoolean(false);

    /** 本次调用新增消息的起始索引（用于终态时只追加新增部分） */
    private int newMsgStartIndex;

    /** 初始消息快照（用于计算本次新增的消息） */
    private List<Message> originalSnapshot;

    public SessionPersister(boolean enableSession,
                            ConversationStore conversationStore,
                            SessionMessageStore sessionMessageStore) {
        this.enableSession = enableSession;
        this.conversationStore = conversationStore;
        this.sessionMessageStore = sessionMessageStore;
    }

    /**
     * 生成 sessionId、写开局记录。
     *
     * @param conversationId 会话窗口 ID（来自 RunnableParams）
     * @param userId         用户 ID（可选）
     * @param query          用户提问
     */
    public void initSession(String conversationId, String userId, String query) {
        sessionId = IdWorker.getId();

        if (enableSession && conversationStore != null && conversationId != null) {
            conversationStore.saveStart(conversationId, sessionId, userId, query);
        }
    }

    /**
     * 记录当前消息快照（在历史消息加载完成后调用）。
     * 终态时用此快照计算本次调用新增的消息。
     */
    public void snapshotMessages(List<Message> messages) {
        this.originalSnapshot = messages != null ? List.copyOf(messages) : List.of();
        this.newMsgStartIndex = this.originalSnapshot.size();
    }

    /**
     * 终态批量落库：写新增消息、更新会话状态。
     * 靠 CAS 保证只写一次。
     *
     * @param messages 当前工作消息列表
     * @param signal   Reactor 信号类型（判断正常完成/错误/取消）
     */
    public void persistOnTerminal(String conversationId, List<Message> messages, SignalType signal) {
        if (conversationId == null || !enableSession || sessionMessageStore == null) {
            return;
        }
        if (!persisted.compareAndSet(false, true)) {
            return;
        }

        String status = switch (signal) {
            case ON_COMPLETE -> "completed";
            case ON_ERROR -> "error";
            case CANCEL -> "interrupted";
            default -> "interrupted";
        };

        try {
            // 计算本次调用新增的消息
            List<Message> thisCallMessages = (originalSnapshot != null && newMsgStartIndex < originalSnapshot.size())
                    ? new ArrayList<>(originalSnapshot.subList(newMsgStartIndex, originalSnapshot.size()))
                    : List.of();

            // 追加原始消息（仅本次新增部分）
            if (!thisCallMessages.isEmpty()) {
                sessionMessageStore.appendMessages(
                        conversationId, sessionId,
                        "original_messages", thisCallMessages);
            }

            // 覆盖写工作消息（全部当前消息）
            sessionMessageStore.replaceMessages(
                    conversationId, sessionId,
                    "working_messages", messages);

            // 更新会话状态
            if (conversationStore != null) {
                conversationStore.updateStatus(sessionId, status);
            }
        } catch (Exception e) {
            log.error("Failed to persist terminal session: {}", e.getMessage());
        }
    }

    // ==================== Getters ====================

    public long getSessionId() {
        return sessionId;
    }

    public boolean isEnableSession() {
        return enableSession;
    }
}
