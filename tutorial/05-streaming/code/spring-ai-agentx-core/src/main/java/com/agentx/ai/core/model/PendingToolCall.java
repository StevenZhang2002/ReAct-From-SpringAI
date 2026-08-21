package com.agentx.ai.core.model;

/**
 * 待处理的工具调用记录。
 * <p>
 * 记录 LLM 返回的工具调用信息，用于后续执行或暂停恢复。
 *
 * @param id        工具调用 ID
 * @param name      工具名称
 * @param arguments 工具参数 JSON
 */
public record PendingToolCall(String id, String name, String arguments) {
}
