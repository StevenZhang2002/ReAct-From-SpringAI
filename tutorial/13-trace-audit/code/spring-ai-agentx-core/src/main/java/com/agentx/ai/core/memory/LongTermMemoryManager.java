package com.agentx.ai.core.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 长期记忆管理器。
 * <p>
 * 封装跨会话记忆的「抽取 - 存储 - 检索」流程。
 *
 * @author bigchui
 */
public class LongTermMemoryManager {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryManager.class);

    public static final String DOC_TYPE = "memory";
    public static final String META_TYPE = "type";
    public static final String META_USER_ID = "user_id";

    private final VectorStore vectorStore;
    private final LongTermMemoryConfig config;

    public LongTermMemoryManager(LongTermMemoryConfig config) {
        this.config = config;
        this.vectorStore = config.getVectorStore();
    }

    // ==================== 写入：存储记忆 ====================

    /**
     * 存储一条记忆。
     */
    public void storeMemory(String userId, String text) {
        if (userId == null || text == null || text.isBlank()) {
            return;
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_TYPE, DOC_TYPE);
        metadata.put(META_USER_ID, userId);
        Document doc = Document.builder()
                .id(UUID.randomUUID().toString())
                .text(text)
                .metadata(metadata)
                .build();
        vectorStore.add(List.of(doc));
        log.debug("Stored memory: userId={}, preview={}", userId, preview(text));
    }

    // ==================== 读取：检索相关记忆 ====================

    /**
     * 按用户和当前 query 检索 top-K 相关记忆。
     */
    public List<Document> searchRelevant(String userId, String query) {
        if (userId == null || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            return vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(config.getTopK())
                    .similarityThreshold(config.getSimilarityThreshold())
                    .build());
        } catch (Exception e) {
            log.warn("Memory search failed: userId={}, err={}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 格式化记忆为可注入的文本。
     */
    public String formatMemories(List<Document> memories) {
        if (memories == null || memories.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[相关记忆]\n");
        for (Document doc : memories) {
            sb.append("- ").append(doc.getText()).append("\n");
        }
        return sb.toString();
    }

    // ==================== 工具方法 ====================

    /**
     * 从对话记录中提取简单记忆（简化版：提取用户消息）。
     */
    public List<String> extractFromTranscript(List<Message> transcript) {
        List<String> memories = new ArrayList<>();
        for (Message msg : transcript) {
            if (msg instanceof UserMessage um) {
                String text = um.getText();
                if (text != null && !text.isBlank() && text.length() < 200) {
                    memories.add("用户说：" + text);
                }
            }
        }
        return memories;
    }

    private static String preview(String text) {
        if (text == null) return "";
        return text.length() > 60 ? text.substring(0, 60) + "..." : text;
    }
}
