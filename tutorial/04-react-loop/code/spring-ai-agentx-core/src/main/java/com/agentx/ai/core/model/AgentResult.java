package com.agentx.ai.core.model;

import com.agentx.ai.core.exception.AgentErrorCode;
import com.agentx.ai.core.exception.AgentException;

/**
 * Agent 执行结果。
 *
 * <p>使用 sealed 接口实现模式匹配，支持两种结果：
 * <ul>
 *   <li>{@link Completed} - 执行完成，包含最终答案</li>
 *   <li>{@link Failed} - 执行失败（如 LLM 调用异常、重试耗尽等）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * AgentResult result = agent.call("你好");
 * switch (result) {
 *     case AgentResult.Completed c -> System.out.println(c.answer());
 *     case AgentResult.Failed f -> System.err.println(f.error());
 * }
 * }</pre>
 *
 * @author bigchui
 */
public sealed interface AgentResult permits AgentResult.Completed, AgentResult.Failed {

    /**
     * 执行完成。
     *
     * @param answer 最终答案文本
     * @param think  思考内容（reasoning_content 或 think 标签内内容），无思考时为 null
     */
    record Completed(String answer, String think) implements AgentResult {

        /**
         * 便捷构造：无思考。
         */
        public Completed(String answer) {
            this(answer, null);
        }
    }

    /**
     * 执行失败（LLM 调用异常、重试耗尽等）。
     *
     * @param error 错误信息
     * @param code  错误码
     */
    record Failed(String error, AgentErrorCode code) implements AgentResult {
    }

    /**
     * 是否执行失败。
     */
    default boolean isFailed() {
        return this instanceof Failed;
    }

    /**
     * 获取最终答案。
     *
     * <p>仅在 {@link Completed} 时返回答案文本。
     * 处于 {@link Failed} 状态时抛出异常。
     *
     * @return 最终答案文本
     * @throws IllegalStateException 如果处于失败状态
     */
    default String answer() {
        if (this instanceof Completed c) {
            return c.answer();
        }
        if (this instanceof Failed f) {
            throw new AgentException(f.code(), f.error());
        }
        throw new IllegalStateException("未知结果类型: " + this);
    }

    /**
     * 获取思考内容。
     *
     * <p>仅在 {@link Completed} 时返回思考内容，可能为 null（模型未输出思考）。
     * 处于 {@link Failed} 状态时抛出异常。
     *
     * @return 思考内容，无思考时返回 null
     * @throws IllegalStateException 如果处于失败状态
     */
    default String think() {
        if (this instanceof Completed c) {
            return c.think();
        }
        if (this instanceof Failed f) {
            throw new AgentException(f.code(), f.error());
        }
        throw new IllegalStateException("未知结果类型: " + this);
    }
}
