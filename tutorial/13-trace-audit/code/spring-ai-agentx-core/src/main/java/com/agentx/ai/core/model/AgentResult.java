package com.agentx.ai.core.model;

import com.agentx.ai.core.exception.AgentErrorCode;

/**
 * Agent 执行结果（sealed 接口）。
 * <p>
 * 本节新增：Paused 变体 — Agent 暂停等待用户输入。
 *
 * @author bigchui
 */
public sealed interface AgentResult permits
        AgentResult.Completed,
        AgentResult.Failed,
        AgentResult.Paused {

    /** 获取最终回答文本（Paused 时返回 null）。 */
    default String answer() {
        return switch (this) {
            case Completed c -> c.answer();
            case Failed f -> null;
            case Paused p -> null;
        };
    }

    /**
     * 执行成功。
     */
    record Completed(String answer) implements AgentResult {
    }

    /**
     * 执行失败。
     */
    record Failed(String message, AgentErrorCode code) implements AgentResult {
    }

    /**
     * Agent 暂停，等待用户输入（本节新增）。
     *
     * @param state 暂停状态快照（用于 resume 恢复）
     */
    record Paused(PauseState state) implements AgentResult {
    }
}
