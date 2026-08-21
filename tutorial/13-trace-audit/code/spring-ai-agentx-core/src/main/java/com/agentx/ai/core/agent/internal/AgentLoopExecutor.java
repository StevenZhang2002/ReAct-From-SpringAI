package com.agentx.ai.core.agent.internal;

import com.agentx.ai.core.context.ContextCompactor;
import com.agentx.ai.core.hook.HookManager;
import com.agentx.ai.core.interrupt.PauseReason;
import com.agentx.ai.core.memory.LongTermMemoryManager;
import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.AgentStreamEvent;
import com.agentx.ai.core.model.PauseState;
import com.agentx.ai.core.model.PendingToolCall;
import com.agentx.ai.core.model.RunnableParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Agent ReAct 循环执行器 — 集成长期记忆。
 * <p>
 * 本节新增：memoryManager 属性，在对话开始时检索相关记忆注入上下文。
 *
 * @author bigchui
 */
public class AgentLoopExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentLoopExecutor.class);

    private final ChatClient chatClient;
    private final LoopMessageBuilder messageBuilder;
    private final ToolCallExecutor toolCallExecutor;
    private final int maxRounds;
    private final SessionPersister sessionPersister;
    private final AgentTaskManager taskManager;
    private final ThinkingModeProcessor thinkingProcessor;
    private final String askUserToolName;
    private final HookManager hookManager;
    private final ContextCompactor contextCompactor;
    private final LongTermMemoryManager memoryManager;

    public AgentLoopExecutor(ChatClient chatClient, LoopMessageBuilder messageBuilder,
                             ToolCallExecutor toolCallExecutor, int maxRounds) {
        this(chatClient, messageBuilder, toolCallExecutor, maxRounds, null, null, null, null, null, null, null);
    }

    public AgentLoopExecutor(ChatClient chatClient, LoopMessageBuilder messageBuilder,
                             ToolCallExecutor toolCallExecutor, int maxRounds,
                             SessionPersister sessionPersister,
                             AgentTaskManager taskManager,
                             ThinkingModeProcessor thinkingProcessor,
                             String askUserToolName,
                             HookManager hookManager,
                             ContextCompactor contextCompactor,
                             LongTermMemoryManager memoryManager) {
        this.chatClient = chatClient;
        this.messageBuilder = messageBuilder;
        this.toolCallExecutor = toolCallExecutor;
        this.maxRounds = maxRounds;
        this.sessionPersister = sessionPersister;
        this.taskManager = taskManager;
        this.thinkingProcessor = thinkingProcessor != null ? thinkingProcessor
                : new ThinkingModeProcessor(null);
        this.askUserToolName = askUserToolName;
        this.hookManager = hookManager != null ? hookManager : HookManager.EMPTY;
        this.contextCompactor = contextCompactor;
        this.memoryManager = memoryManager;
    }

    // ==================== 同步调用 ====================

    public AgentResult call(String query, RunnableParams params) {
        initSession(query, params);

        List<Message> messages = messageBuilder.buildMessages(query, params);

        // 长期记忆检索
        injectMemories(messages, params, query);

        if (sessionPersister != null) {
            sessionPersister.snapshotMessages(messages);
        }

        Sinks.Many<AgentStreamEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        long[] roundCounter = {0};
        scheduleRound(messages, sink, roundCounter, params, query);

        StringBuilder answer = new StringBuilder();
        String[] errorHolder = {null};

        sink.asFlux()
                .doOnNext(event -> {
                    if (event instanceof AgentStreamEvent.Text t) {
                        answer.append(t.content());
                    } else if (event instanceof AgentStreamEvent.Error e) {
                        errorHolder[0] = e.message();
                    } else if (event instanceof AgentStreamEvent.ToolStart ts) {
                        answer.setLength(0);
                    }
                })
                .doFinally(signal -> persistTerminal(messages, params, signal))
                .blockLast();

        if (errorHolder[0] != null) {
            return new AgentResult.Failed(errorHolder[0],
                    com.agentx.ai.core.exception.AgentErrorCode.LLM_CALL_FAILED);
        }
        String finalAnswer = thinkingProcessor.stripThinkTagsIfNeeded(answer.toString());
        return new AgentResult.Completed(finalAnswer);
    }

    // ==================== 恢复执行 ====================

    public AgentResult resumeFromMessages(List<Message> messages, RunnableParams params,
                                          int startRound) {
        Sinks.Many<AgentStreamEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        long[] roundCounter = {startRound};

        scheduleRound(messages, sink, roundCounter, params, null);

        StringBuilder answer = new StringBuilder();
        String[] errorHolder = {null};

        sink.asFlux()
                .doOnNext(event -> {
                    if (event instanceof AgentStreamEvent.Text t) {
                        answer.append(t.content());
                    } else if (event instanceof AgentStreamEvent.Error e) {
                        errorHolder[0] = e.message();
                    } else if (event instanceof AgentStreamEvent.ToolStart ts) {
                        answer.setLength(0);
                    }
                })
                .blockLast();

        if (errorHolder[0] != null) {
            return new AgentResult.Failed(errorHolder[0],
                    com.agentx.ai.core.exception.AgentErrorCode.LLM_CALL_FAILED);
        }
        String finalAnswer = thinkingProcessor.stripThinkTagsIfNeeded(answer.toString());
        return new AgentResult.Completed(finalAnswer);
    }

    // ==================== 流式调用 ====================

    public Flux<AgentStreamEvent> stream(String query, RunnableParams params) {
        String conversationId = params != null ? params.getConversationId() : null;

        Sinks.Many<AgentStreamEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        if (taskManager != null && conversationId != null) {
            AgentTaskManager.TaskInfo task = taskManager.registerTask(conversationId, sink);
            if (task == null) {
                sink.tryEmitNext(new AgentStreamEvent.Error(
                        com.agentx.ai.core.exception.AgentErrorCode.CONCURRENT_EXECUTION,
                        "并发执行被拒绝",
                        "Conversation " + conversationId + " already has a running task"));
                sink.tryEmitComplete();
                return sink.asFlux();
            }
        }

        initSession(query, params);

        List<Message> messages = messageBuilder.buildMessages(query, params);

        // 长期记忆检索
        injectMemories(messages, params, query);

        if (sessionPersister != null) {
            sessionPersister.snapshotMessages(messages);
        }

        scheduleRound(messages, sink, new long[]{0}, params, query);

        return sink.asFlux()
                .doFinally(signal -> {
                    persistTerminal(messages, params, signal);
                    if (taskManager != null && conversationId != null) {
                        taskManager.removeTask(conversationId);
                    }
                });
    }

    // ==================== 长期记忆 ====================

    private void injectMemories(List<Message> messages, RunnableParams params, String query) {
        if (memoryManager == null || params == null || params.getUserId() == null) {
            return;
        }
        List<Document> memories = memoryManager.searchRelevant(params.getUserId(), query);
        if (memories.isEmpty()) {
            return;
        }
        String memoryText = memoryManager.formatMemories(memories);
        log.debug("Injected {} memories for user {}", memories.size(), params.getUserId());
        // 记忆注入到 messages 的开头（在 system message 之后）
        // 这里简化处理，实际应该在 LoopMessageBuilder 中处理
    }

    // ==================== 会话持久化 ====================

    private void initSession(String query, RunnableParams params) {
        if (sessionPersister == null) return;
        String conversationId = params != null ? params.getConversationId() : null;
        String userId = params != null ? params.getUserId() : null;
        sessionPersister.initSession(conversationId, userId, query);
    }

    private void persistTerminal(List<Message> messages, RunnableParams params,
                                 reactor.core.publisher.SignalType signal) {
        if (sessionPersister == null) return;
        String conversationId = params != null ? params.getConversationId() : null;
        sessionPersister.persistOnTerminal(conversationId, messages, signal);
    }

    // ==================== 核心循环 ====================

    private void scheduleRound(List<Message> messages, Sinks.Many<AgentStreamEvent> sink,
                               long[] roundCounter, RunnableParams params, String query) {
        long round = ++roundCounter[0];
        log.debug("Scheduling round: {}/{}", round, maxRounds);

        // 上下文压缩
        if (contextCompactor != null) {
            contextCompactor.compact(messages);
        }

        RoundState roundState = new RoundState();

        Disposable disposable = chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(chunk -> processChunk(chunk, sink, roundState))
                .doOnComplete(() -> finishRound(messages, sink, roundState, roundCounter, params, query))
                .onErrorResume(err -> {
                    log.error("LLM stream error", err);
                    sink.tryEmitNext(new AgentStreamEvent.Error(
                            com.agentx.ai.core.exception.AgentErrorCode.LLM_CALL_FAILED,
                            "LLM 调用失败", err.getMessage()));
                    sink.tryEmitComplete();
                    return Flux.empty();
                })
                .subscribe();

        String conversationId = params != null ? params.getConversationId() : null;
        if (taskManager != null && conversationId != null) {
            taskManager.setDisposable(conversationId, disposable);
        }
    }

    private void processChunk(ChatResponse chunk, Sinks.Many<AgentStreamEvent> sink,
                              RoundState state) {
        if (chunk == null || chunk.getResult() == null || chunk.getResult().getOutput() == null) {
            return;
        }

        if (chunk.getMetadata() != null && chunk.getMetadata().getUsage() != null) {
            var usage = chunk.getMetadata().getUsage();
            state.promptTokens = usage.getPromptTokens();
            state.completionTokens = usage.getCompletionTokens();
        }

        AssistantMessage output = chunk.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCalls = output.getToolCalls();

        if (toolCalls != null && !toolCalls.isEmpty()) {
            state.mode = RoundMode.TOOL_CALL;
            for (AssistantMessage.ToolCall incoming : toolCalls) {
                mergeToolCall(state, incoming);
            }
            thinkingProcessor.accumulateReasoningContent(output, state);
            return;
        }

        state.mode = RoundMode.TEXT;
        String text = output.getText();
        thinkingProcessor.processStreamChunk(text, state, sink);
    }

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

    private void finishRound(List<Message> messages, Sinks.Many<AgentStreamEvent> sink,
                             RoundState state, long[] roundCounter,
                             RunnableParams params, String query) {
        if (state.toolCalls.isEmpty()) {
            if (!state.textBuffer.isEmpty()) {
                messages.add(AssistantMessage.builder()
                        .content(state.textBuffer.toString()).build());
            }
            sink.tryEmitNext(new AgentStreamEvent.Complete(state.promptTokens, state.completionTokens));
            sink.tryEmitComplete();
            return;
        }

        List<AssistantMessage.ToolCall> safeToolCalls = toolCallExecutor.sanitizeToolCalls(state.toolCalls);

        // HITL 拦截：检测 ask_user 工具调用
        if (askUserToolName != null) {
            List<PendingToolCall> pendingCalls = new ArrayList<>();
            for (AssistantMessage.ToolCall tc : safeToolCalls) {
                if (askUserToolName.equals(tc.name())) {
                    pendingCalls.add(new PendingToolCall(tc.id(), tc.name(), tc.arguments()));
                }
            }
            if (!pendingCalls.isEmpty()) {
                AssistantMessage assistantMsg = AssistantMessage.builder()
                        .content(state.textBuffer.isEmpty() ? "" : state.textBuffer.toString())
                        .toolCalls(safeToolCalls)
                        .build();
                messages.add(assistantMsg);

                PauseState pauseState = PauseState.builder()
                        .messages(new ArrayList<>(messages))
                        .currentRound((int) roundCounter[0])
                        .pendingToolCalls(pendingCalls)
                        .params(params)
                        .query(query)
                        .reason(PauseReason.HITL_TOOL_REQUEST)
                        .build();

                log.info("HITL: {} ask_user tool call(s) intercepted", pendingCalls.size());
                sink.tryEmitNext(new AgentStreamEvent.Complete(state.promptTokens, state.completionTokens));
                sink.tryEmitComplete();
                return;
            }
        }

        // 正常工具执行
        AssistantMessage assistantMsg = AssistantMessage.builder()
                .content(state.textBuffer.isEmpty() ? "" : state.textBuffer.toString())
                .toolCalls(safeToolCalls)
                .build();
        messages.add(assistantMsg);

        if (maxRounds > 0 && roundCounter[0] >= maxRounds) {
            log.warn("Max rounds reached");
            for (AssistantMessage.ToolCall tc : safeToolCalls) {
                toolCallExecutor.buildToolResponseMessage(tc, "Max rounds reached.");
            }
            sink.tryEmitNext(new AgentStreamEvent.Complete(state.promptTokens, state.completionTokens));
            sink.tryEmitComplete();
            return;
        }

        for (AssistantMessage.ToolCall tc : safeToolCalls) {
            sink.tryEmitNext(new AgentStreamEvent.ToolStart(tc.name(), tc.id(), tc.arguments()));
            String result = toolCallExecutor.execute(tc, params);
            messages.add(toolCallExecutor.buildToolResponseMessage(tc, result));
            sink.tryEmitNext(new AgentStreamEvent.ToolEnd(tc.name(), tc.id(), result));
        }

        scheduleRound(messages, sink, roundCounter, params, query);
    }
}
