package com.agentx.ai.core.agent.internal;

import com.agentx.ai.core.model.AgentStreamEvent;
import com.agentx.ai.core.model.ThinkingMode;
import com.agentx.ai.core.stage.ThinkTagParser;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Sinks;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * ThinkingMode 处理器 — 封装思考模式的分支逻辑。
 * <p>
 * 支持三种模式：DISABLED / THINK_TAG / REASONING_CONTENT。
 *
 * @author bigchui
 */
public final class ThinkingModeProcessor {

    private final ThinkingMode thinkingMode;

    private volatile Method cachedReasoningMethod;
    private volatile Class<?> cachedReasoningClass;

    public ThinkingModeProcessor(ThinkingMode thinkingMode) {
        this.thinkingMode = thinkingMode != null ? thinkingMode : ThinkingMode.DISABLED;
    }

    /**
     * 处理流式文本 chunk（content 字段）。
     */
    public void processStreamChunk(String text, RoundState state,
                                   Sinks.Many<AgentStreamEvent> sink) {
        if (thinkingMode == ThinkingMode.REASONING_CONTENT) {
            // reasoning_content 模式：content 就是正式回答
            if (text != null && !text.isEmpty()) {
                state.textBuffer.append(text);
                sink.tryEmitNext(new AgentStreamEvent.Text(text));
            }
        } else if (thinkingMode == ThinkingMode.THINK_TAG) {
            // think 标签模式：解析标签，发射 Thinking/Text 事件
            processThinkTagChunk(text, state, sink, true);
        } else {
            // 默认模式：解析并剥离 think 标签，不发射 Thinking 事件
            processThinkTagChunk(text, state, sink, false);
        }
    }

    /**
     * 处理 reasoning_content chunk（独立字段）。
     */
    public void processReasoningChunk(String reasoning, RoundState state,
                                      Sinks.Many<AgentStreamEvent> sink) {
        if (reasoning != null && !reasoning.isEmpty()) {
            state.reasoningBuffer.append(reasoning);
            sink.tryEmitNext(new AgentStreamEvent.Thinking(reasoning));
        }
    }

    /**
     * 去除 think 标签（DISABLED/THINK_TAG 模式需要，REASONING_CONTENT 已是纯正文）。
     */
    public String stripThinkTagsIfNeeded(String answer) {
        if (thinkingMode != ThinkingMode.REASONING_CONTENT && answer != null) {
            return ThinkTagParser.stripThinkTags(answer);
        }
        return answer;
    }

    /**
     * 从 AssistantMessage 提取 reasoning_content（metadata 优先，反射兜底）。
     */
    public String extractReasoningContent(AssistantMessage msg) {
        Map<String, Object> metadata = msg.getMetadata();
        if (metadata != null) {
            Object rc = metadata.get("reasoningContent");
            if (rc == null) {
                rc = metadata.get("reasoning_content");
            }
            if (rc instanceof String s && !s.isEmpty()) {
                return s;
            }
        }

        // 反射兜底
        Class<?> msgClass = msg.getClass();
        try {
            if (cachedReasoningMethod == null || cachedReasoningClass != msgClass) {
                try {
                    cachedReasoningMethod = msgClass.getMethod("getReasoningContent");
                    cachedReasoningClass = msgClass;
                } catch (NoSuchMethodException e) {
                    cachedReasoningClass = null;
                    return null;
                }
            }
            if (cachedReasoningMethod != null && cachedReasoningClass != null) {
                Object result = cachedReasoningMethod.invoke(msg);
                if (result instanceof String s && !s.isEmpty()) {
                    return s;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * 累积 reasoning_content 到 RoundState。
     */
    public void accumulateReasoningContent(AssistantMessage msg, RoundState state) {
        if (thinkingMode != ThinkingMode.REASONING_CONTENT) {
            return;
        }
        String reasoning = extractReasoningContent(msg);
        if (reasoning != null && !reasoning.isEmpty()) {
            state.reasoningBuffer.append(reasoning);
        }
    }

    private void processThinkTagChunk(String text, RoundState state,
                                      Sinks.Many<AgentStreamEvent> sink,
                                      boolean emitThinkingEvents) {
        if (text == null || text.isEmpty()) {
            return;
        }
        ThinkTagParser.ParseResult result = ThinkTagParser.parse(text, state.inThink);
        state.inThink = result.inThink();
        for (ThinkTagParser.Segment seg : result.segments()) {
            if (seg.thinking()) {
                if (emitThinkingEvents) {
                    sink.tryEmitNext(new AgentStreamEvent.Thinking(seg.content()));
                }
                state.reasoningBuffer.append(seg.content());
            } else {
                state.textBuffer.append(seg.content());
                sink.tryEmitNext(new AgentStreamEvent.Text(seg.content()));
            }
        }
    }
}
