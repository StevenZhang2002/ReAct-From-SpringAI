package com.agentx.ai.core.memory.store;

import com.agentx.ai.core.utils.MessageJsonSerializer;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话消息链存储 — agentx_session 表的 CRUD。
 * <p>
 * 每条消息一行，按 conversation_id + state_key 聚合，item_index 保留顺序。
 * 终态时批量追加本次调用新增的消息，ReAct 循环中不触碰 DB。
 *
 * <h2>state_key 说明</h2>
 * <ul>
 *   <li>{@code original_messages} — 原始消息（不压缩），仅追加</li>
 *   <li>{@code working_messages} — 工作消息（可能被压缩），覆盖写</li>
 * </ul>
 *
 * @author bigchui
 */
public class SessionMessageStore {

    private static final Logger log = LoggerFactory.getLogger(SessionMessageStore.class);

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE agentx_session (
                id              BIGINT       NOT NULL  COMMENT '主键ID',
                conversation_id VARCHAR(100) NOT NULL  COMMENT '会话窗口ID',
                session_id      VARCHAR(100) NOT NULL  COMMENT '本次调用ID',
                state_key       VARCHAR(255) NOT NULL  COMMENT '状态键: original_messages / working_messages',
                item_index      INT          NOT NULL DEFAULT 0 COMMENT '消息在状态键内的序号',
                state_data      LONGTEXT     NOT NULL  COMMENT '消息JSON',
                created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (id)
            ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AgentX会话消息链表'
            """;

    private static final String CREATE_IDX_CONV_SQL = """
            CREATE INDEX idx_conv_state ON agentx_session (conversation_id, state_key)
            """;

    private static final String INSERT_SQL = """
            INSERT INTO agentx_session (id, conversation_id, session_id, state_key, item_index, state_data, created_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

    private static final String SELECT_SQL = """
            SELECT state_data FROM agentx_session
            WHERE conversation_id = ? AND state_key = ?
            ORDER BY item_index ASC, id ASC
            """;

    private static final String MAX_INDEX_SQL = """
            SELECT COALESCE(MAX(item_index), -1) FROM agentx_session
            WHERE conversation_id = ? AND state_key = ?
            """;

    private static final String DELETE_BY_CONV_KEY_SQL = """
            DELETE FROM agentx_session
            WHERE conversation_id = ? AND state_key = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private volatile boolean initialized = false;

    public SessionMessageStore(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void initialize() {
        ensureInitialized();
    }

    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    try {
                        jdbcTemplate.execute(CREATE_TABLE_SQL);
                    } catch (Exception e) {
                        log.debug("Table agentx_session creation skipped (may already exist): {}", e.getMessage());
                    }
                    try {
                        jdbcTemplate.execute(CREATE_IDX_CONV_SQL);
                    } catch (Exception e) {
                        log.debug("Index idx_conv_state skipped: {}", e.getMessage());
                    }
                    initialized = true;
                    log.info("agentx_session table initialized");
                }
            }
        }
    }

    /**
     * 加载某会话窗口指定状态键的全部消息，按 item_index 顺序合并。
     * 用于多轮对话加载历史上下文。
     */
    public List<Message> getMessages(String conversationId, String stateKey) {
        if (conversationId == null || stateKey == null) {
            return new ArrayList<>();
        }
        ensureInitialized();
        List<String> jsonList = jdbcTemplate.queryForList(
                SELECT_SQL, String.class, conversationId, stateKey);

        List<Message> all = new ArrayList<>(jsonList.size() * 2);
        for (String json : jsonList) {
            if (json != null && !json.isBlank()) {
                all.addAll(MessageJsonSerializer.fromJson(json));
            }
        }
        return all;
    }

    /**
     * 批量追加消息。item_index 从当前最大值 +1 起递增。
     * 终态时调用：一次调用新增的消息一次性写入，避免 ReAct 循环中频繁 DB I/O。
     * system 消息属于运行时外部上下文，不进入 agentx_session。
     */
    public void appendMessages(String conversationId, long sessionId,
                               String stateKey, List<Message> messages) {
        if (conversationId == null || stateKey == null || messages == null || messages.isEmpty()) {
            return;
        }
        List<Message> persistedMessages = filterPersistableMessages(messages);
        if (persistedMessages.isEmpty()) {
            return;
        }
        ensureInitialized();

        Integer maxIndex = jdbcTemplate.queryForObject(MAX_INDEX_SQL, Integer.class, conversationId, stateKey);
        int startIndex = (maxIndex == null ? -1 : maxIndex) + 1;

        String sessionIdStr = String.valueOf(sessionId);
        List<Object[]> batchArgs = new ArrayList<>(persistedMessages.size());
        for (int i = 0; i < persistedMessages.size(); i++) {
            String data = MessageJsonSerializer.toJson(List.of(persistedMessages.get(i)));
            batchArgs.add(new Object[]{
                    IdWorker.getId(), conversationId, sessionIdStr, stateKey, startIndex + i, data
            });
        }
        jdbcTemplate.batchUpdate(INSERT_SQL, batchArgs);
        log.debug("Appended {} messages: conversationId={}, sessionId={}, stateKey={}, startIndex={}",
                persistedMessages.size(), conversationId, sessionId, stateKey, startIndex);
    }

    /**
     * 覆盖写：先删除指定 conversationId+stateKey 的全部行，再批量插入。
     * 用于 working_messages 等需要随压缩演进的视图状态。
     */
    public void replaceMessages(String conversationId, long sessionId,
                                String stateKey, List<Message> messages) {
        if (conversationId == null || stateKey == null) {
            return;
        }
        List<Message> persistedMessages = filterPersistableMessages(messages);
        ensureInitialized();
        jdbcTemplate.update(DELETE_BY_CONV_KEY_SQL, conversationId, stateKey);
        appendMessages(conversationId, sessionId, stateKey, persistedMessages);
        log.debug("Replaced state: conversationId={}, stateKey={}, rows={}",
                conversationId, stateKey, persistedMessages.size());
    }

    /**
     * 过滤掉 SystemMessage — system 消息属于运行时外部上下文，不持久化。
     */
    private List<Message> filterPersistableMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<Message> persisted = new ArrayList<>(messages.size());
        for (Message message : messages) {
            if (!(message instanceof SystemMessage)) {
                persisted.add(message);
            }
        }
        return persisted;
    }
}
