package com.agentx.ai.core.agent.internal;

import com.agentx.ai.core.exception.AgentErrorCode;
import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.AgentStreamEvent;
import com.agentx.ai.core.model.RunnableParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Agent ReAct 循环执行器 — 流式版本。
 * <p>
 * 本节核心升级：新增 stream() 方法，基于 Reactor Sink 实现流式输出。
 * <p>
 * 流式流程：
 * <ol>
 *   <li>创建 Sink</li>
 *   <li>scheduleRound：调用 LLM 流式接口</li>
 *   <li>processChunk：逐 chunk 处理，发射 Text/ToolStart/ToolEnd 事件</li>
 *   <li>finishRound：判断是否有工具调用，有则执行后开始下一轮，无则 Complete</li>
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

    // ==================== 同步调用 ====================

    public AgentResult call(String query, RunnableParams params) {
        List<Message> messages = messageBuilder.buildMessages(query, params);
        Sinks.Many<AgentStreamEvent> sink = Sinks.many().unicast().onBackpressureBuffer();

        scheduleRound(messages, sink, new long[]{0}, params, query);

        // 阻塞收集结果
        StringBuilder answer = new StringBuilder();
        String[] errorHolder = {null};

        sink.asFlux()
                .doOnNext(event -> {
                    if (event instanceof AgentStreamEvent.Text t) {
                        answer.append(t.content());
                    } else if (event instanceof AgentStreamEvent.Error e) {
                        errorHolder[0] = e.message();
                    } else if (event instanceof AgentStreamEvent.ToolStart ts) {
                        answer.setLength(0); // 工具调用轮清空 answer
                    }
                })
                .blockLast();

        if (errorHolder[0] != null) {
            return new AgentResult.Failed(errorHolder[0], AgentErrorCode.LLM_CALL_FAILED);
        }
        return new AgentResult.Completed(answer.toString());
    }

    // ==================== 流式调用 ====================

    /**
     * 流式执行 ReAct 循环。
     */
    public Flux<AgentStreamEvent> stream(String query, RunnableParams params) {
        List<Message> messages = messageBuilder.buildMessages(query, params);
        Sinks.Many<AgentStreamEvent> sink = Sinks.many().unicast().onBackpressureBuffer();

        scheduleRound(messages, sink, new long[]{0}, params, query);

        return sink.asFlux();
    }

    // ==================== 核心循环 ====================

    /**
     * 调度一轮 LLM 调用（流式）。
     */
    private void scheduleRound(List<Message> messages, Sinks.Many<AgentStreamEvent> sink,
                               long[] roundCounter, RunnableParams params, String query) {
        long round = ++roundCounter[0];
        log.debug("Scheduling round: {}/{}", round, maxRounds);

        RoundState roundState = new RoundState();

        chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(chunk -> processChunk(chunk, sink, roundState))
                .doOnComplete(() -> finishRound(messages, sink, roundState, roundCounter, params, query))
                .onErrorResume(err -> {
                    log.error("LLM stream error", err);
                    sink.tryEmitNext(new AgentStreamEvent.Error(
                            AgentErrorCode.LLM_CALL_FAILED, "LLM 调用失败", err.getMessage()));
                    sink.tryEmitComplete();
                    return Flux.empty();
                })
                .subscribe();
    }

    /**
     * 处理单个流式 chunk。
     */
    private void processChunk(ChatResponse chunk, Sinks.Many<AgentStreamEvent> sink,
                              RoundState state) {
        if (chunk == null || chunk.getResult() == null || chunk.getResult().getOutput() == null) {
            return;
        }

        // 捕获 token 用量
        if (chunk.getMetadata() != null && chunk.getMetadata().getUsage() != null) {
            var usage = chunk.getMetadata().getUsage();
            state.promptTokens = usage.getPromptTokens();
            state.completionTokens = usage.getCompletionTokens();
        }

        List<AssistantMessage.ToolCall> toolCalls = chunk.getResult().getOutput().getToolCalls();

        if (toolCalls != null && !toolCalls.isEmpty()) {
            state.mode = RoundMode.TOOL_CALL;
            for (AssistantMessage.ToolCall incoming : toolCalls) {
                mergeToolCall(state, incoming);
            }
            return;
        }

        // 文本 chunk
        state.mode = RoundMode.TEXT;
        String text = chunk.getResult().getOutput().getText();
        if (text != null && !text.isEmpty()) {
            state.textBuffer.append(text);
            sink.tryEmitNext(new AgentStreamEvent.Text(text));
        }
    }

    /**
     * 合并流式工具调用（按 id 拼接 arguments）。
     */
    private void mergeToolCall(RoundState state, AssistantMessage.ToolCall incoming) {
        for (int i = 0; i < state.toolCalls.size(); i++) {
            AssistantMessage.ToolCall existing = state.toolCalls.get(i);
            if (existing.id().equals(incoming.id())) {
                String mergedArgs = Objects.toString(existing.arguments(), "") +
                        Objects.toString(incoming.arguments(), "");
                state.toolCalls.set(i, new AssistantMessage.ToolCall(
                        existing.id(), existing.type() != null ? existing.type() : "function",
                        existing.name(), mergedArgs));
                return;
            }
        }
        state.toolCalls.add(incoming);
    }

    /**
     * 一轮结束后的处理。
     */
    private void finishRound(List<Message> messages, Sinks.Many<AgentStreamEvent> sink,
                             RoundState state, long[] roundCounter,
                             RunnableParams params, String query) {
        // 无工具调用 → 最终答案
        if (state.toolCalls.isEmpty()) {
            if (!state.textBuffer.isEmpty()) {
                messages.add(AssistantMessage.builder()
                        .content(state.textBuffer.toString()).build());
            }
            sink.tryEmitNext(new AgentStreamEvent.Complete(state.promptTokens, state.completionTokens));
            sink.tryEmitComplete();
            return;
        }

        // 有工具调用
        List<AssistantMessage.ToolCall> safeToolCalls = toolCallExecutor.sanitizeToolCalls(state.toolCalls);
        AssistantMessage assistantMsg = AssistantMessage.builder()
                .content(state.textBuffer.isEmpty() ? "" : state.textBuffer.toString())
                .toolCalls(safeToolCalls)
                .build();
        messages.add(assistantMsg);

        // 达到上限
        if (maxRounds > 0 && roundCounter[0] >= maxRounds) {
            log.warn("Max rounds reached");
            for (AssistantMessage.ToolCall tc : safeToolCalls) {
                toolCallExecutor.buildToolResponseMessage(tc, "Max rounds reached.");
            }
            sink.tryEmitNext(new AgentStreamEvent.Complete(state.promptTokens, state.completionTokens));
            sink.tryEmitComplete();
            return;
        }

        // 执行工具
        for (AssistantMessage.ToolCall tc : safeToolCalls) {
            sink.tryEmitNext(new AgentStreamEvent.ToolStart(tc.name(), tc.id(), tc.arguments()));
            String result = toolCallExecutor.execute(tc, params);
            messages.add(toolCallExecutor.buildToolResponseMessage(tc, result));
            sink.tryEmitNext(new AgentStreamEvent.ToolEnd(tc.name(), tc.id(), result));
        }

        // 继续下一轮
        scheduleRound(messages, sink, roundCounter, params, query);
    }
}
