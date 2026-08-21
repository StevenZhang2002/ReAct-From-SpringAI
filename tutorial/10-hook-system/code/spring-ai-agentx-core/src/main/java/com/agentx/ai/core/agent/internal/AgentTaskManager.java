package com.agentx.ai.core.agent.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 任务管理器（简化版）。
 * <p>
 * 管理流式输出的停止和资源清理，防止同一会话并发执行。
 * <p>
 * 本节实现基础功能：
 * <ul>
 *   <li>任务注册（registerTask）— 同一 conversationId 不能并发</li>
 *   <li>任务停止（stopTask）— dispose + complete</li>
 *   <li>任务清理（removeTask）— 正常结束后移除</li>
 * </ul>
 * <p>
 * 后续章节扩展：
 * <ul>
 *   <li>第 09 节：interrupt() — 用户主动中断 + PauseState 快照</li>
 * </ul>
 *
 * @author bigchui
 */
public class AgentTaskManager {

    private static final Logger log = LoggerFactory.getLogger(AgentTaskManager.class);

    private final Map<String, TaskInfo> taskMap = new ConcurrentHashMap<>();

    /**
     * 任务信息。
     */
    public static class TaskInfo {
        private final Sinks.Many<?> sink;
        private volatile Disposable disposable;
        private final long createTime;

        TaskInfo(Sinks.Many<?> sink) {
            this.sink = sink;
            this.createTime = System.currentTimeMillis();
        }

        public Sinks.Many<?> getSink() {
            return sink;
        }

        public Disposable getDisposable() {
            return disposable;
        }

        public void setDisposable(Disposable disposable) {
            this.disposable = disposable;
        }

        public long getCreateTime() {
            return createTime;
        }
    }

    /**
     * 注册任务。同一 conversationId 不能并发执行。
     *
     * @param conversationId 会话 ID
     * @param sink           事件 Sink
     * @return TaskInfo 注册成功；null 表示已有任务在运行
     */
    public TaskInfo registerTask(String conversationId, Sinks.Many<?> sink) {
        if (conversationId == null) {
            return new TaskInfo(sink);
        }
        TaskInfo newTask = new TaskInfo(sink);
        TaskInfo existing = taskMap.putIfAbsent(conversationId, newTask);
        if (existing != null) {
            log.warn("Task already exists for conversation: {}", conversationId);
            return null;
        }
        log.debug("Registered task for conversation: {}", conversationId);
        return newTask;
    }

    /**
     * 设置底层 Disposable（LLM 订阅）。
     */
    public void setDisposable(String conversationId, Disposable disposable) {
        if (conversationId == null) return;
        TaskInfo taskInfo = taskMap.get(conversationId);
        if (taskInfo != null) {
            taskInfo.setDisposable(disposable);
        } else {
            // 任务已被移除，直接 dispose 防止泄漏
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
                log.debug("Task already removed, disposed orphaned subscription: {}", conversationId);
            }
        }
    }

    /**
     * 强制停止任务，丢弃所有运行时状态。
     *
     * @param conversationId 会话 ID
     * @return true 如果任务存在并已停止
     */
    public boolean stopTask(String conversationId) {
        if (conversationId == null) return false;
        TaskInfo taskInfo = taskMap.remove(conversationId);
        if (taskInfo == null) {
            log.warn("No running task for conversation: {}", conversationId);
            return false;
        }

        try {
            Disposable disposable = taskInfo.getDisposable();
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
                log.debug("Disposed underlying call for conversation: {}", conversationId);
            }

            var sink = taskInfo.getSink();
            if (sink != null) {
                sink.tryEmitComplete();
                log.debug("Completed stream output for conversation: {}", conversationId);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to stop task for conversation: {}", conversationId, e);
            return false;
        }
    }

    /**
     * 正常结束后移除任务。
     */
    public void removeTask(String conversationId) {
        if (conversationId == null) return;
        TaskInfo removed = taskMap.remove(conversationId);
        if (removed != null) {
            log.debug("Removed task for conversation: {}", conversationId);
        }
    }

    /**
     * 检查是否有正在运行的任务。
     */
    public boolean hasRunningTask(String conversationId) {
        if (conversationId == null) return false;
        return taskMap.containsKey(conversationId);
    }

    /**
     * 获取当前任务数。
     */
    public int getTaskCount() {
        return taskMap.size();
    }
}
