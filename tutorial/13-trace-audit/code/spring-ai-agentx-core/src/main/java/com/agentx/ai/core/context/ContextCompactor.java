package com.agentx.ai.core.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下文压缩器。
 * <p>
 * 当消息数或 token 数超过阈值时，压缩旧消息（保留系统消息和最近的消息）。
 *
 * @author bigchui
 */
public class ContextCompactor {

    private static final Logger log = LoggerFactory.getLogger(ContextCompactor.class);

    private final ContextPolicy policy;

    public ContextCompactor(ContextPolicy policy) {
        this.policy = policy != null ? policy : ContextPolicy.defaults();
    }

    /**
     * 压缩消息列表（如果超过阈值）。
     *
     * @param messages 消息列表（会被修改）
     */
    public void compact(List<Message> messages) {
        if (messages == null || messages.size() <= 2) {
            return;
        }

        // 检查外层门禁
        if (!shouldCompress(messages)) {
            return;
        }

        log.info("Context compression triggered: messages={}, tokens={}",
                messages.size(), TokenEstimator.estimateTokens(messages));

        // 保留系统消息和最近的消息
        List<Message> preserved = new ArrayList<>();
        List<Message> toCompress = new ArrayList<>();

        int lastKeep = Math.min(policy.lastKeep(), messages.size() - 1);
        int keepFrom = messages.size() - lastKeep;

        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg instanceof SystemMessage) {
                preserved.add(msg);
            } else if (i >= keepFrom) {
                preserved.add(msg);
            } else {
                toCompress.add(msg);
            }
        }

        if (toCompress.isEmpty()) {
            return;
        }

        // 简单压缩：生成摘要消息
        String summary = generateSummary(toCompress);
        UserMessage summaryMsg = new UserMessage("[历史对话摘要] " + summary);

        // 重建消息列表
        messages.clear();
        messages.add(summaryMsg);
        messages.addAll(preserved);

        log.info("Context compression completed: before={}, after={}, tokens={}",
                toCompress.size() + preserved.size(), messages.size(),
                TokenEstimator.estimateTokens(messages));
    }

    /**
     * 检查是否需要压缩。
     */
    private boolean shouldCompress(List<Message> messages) {
        if (messages.size() >= policy.msgThreshold()) {
            return true;
        }
        int estimatedTokens = TokenEstimator.estimateTokens(messages);
        return estimatedTokens >= policy.tokenThreshold();
    }

    /**
     * 生成消息摘要（简化版）。
     */
    private String generateSummary(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        int userCount = 0;
        int assistantCount = 0;
        int toolCount = 0;

        for (Message msg : messages) {
            if (msg instanceof UserMessage) {
                userCount++;
            } else if (msg instanceof AssistantMessage am) {
                assistantCount++;
                if (am.getToolCalls() != null && !am.getToolCalls().isEmpty()) {
                    toolCount += am.getToolCalls().size();
                }
            }
        }

        sb.append(String.format("共 %d 轮对话（用户 %d 次，助手 %d 次",
                messages.size(), userCount, assistantCount));
        if (toolCount > 0) {
            sb.append(String.format("，工具调用 %d 次", toolCount));
        }
        sb.append("）。");

        // 添加最后几条用户消息的预览
        List<UserMessage> lastUserMessages = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0 && lastUserMessages.size() < 3; i--) {
            if (messages.get(i) instanceof UserMessage um) {
                lastUserMessages.add(0, um);
            }
        }

        if (!lastUserMessages.isEmpty()) {
            sb.append(" 最近话题：");
            for (UserMessage um : lastUserMessages) {
                String text = um.getText();
                if (text.length() > 50) {
                    text = text.substring(0, 50) + "...";
                }
                sb.append("\"").append(text).append("\" ");
            }
        }

        return sb.toString();
    }

    public ContextPolicy getPolicy() {
        return policy;
    }
}
