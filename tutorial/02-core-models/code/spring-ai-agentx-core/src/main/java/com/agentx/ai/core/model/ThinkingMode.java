package com.agentx.ai.core.model;

/**
 * 思考模型的输出格式。
 * <p>
 * 不同厂商的思考模型通过不同方式返回推理过程，调用方需根据模型类型选择对应模式。
 */
public enum ThinkingMode {

    /**
     * 不处理思考内容（默认）。
     */
    DISABLED,

    /**
     * &lt;think/&gt; 标签格式。
     * <p>
     * 思考内容嵌入在 content 字段中，用 {@code <think>...</think>} 标签包裹。
     * 适用于：MiniMax M2.7。
     */
    THINK_TAG,

    /**
     * {@code reasoning_content} 字段格式。
     * <p>
     * 思考内容通过独立的 {@code reasoning_content} 字段返回，{@code content} 仅包含正式回答。
     * 适用于：DeepSeek、Qwen3.6-plus 等。
     */
    REASONING_CONTENT
}
