package com.agentx.ai.core.agent.internal;

import com.agentx.ai.core.model.RunnableParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具调用执行器 — 负责工具执行和结果消息组装。
 * <p>
 * 本节为最简版本：同步执行工具，无参数替换，无 Hook。
 * 后续章节逐步添加：
 * <ul>
 *   <li>第 08 节：toolParams 参数替换</li>
 *   <li>第 11 节：BeforeToolExecution / AfterToolExecution Hook</li>
 * </ul>
 *
 * @author bigchui
 */
public class ToolCallExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolCallExecutor.class);

    private final Map<String, ToolCallback> toolMap;

    public ToolCallExecutor(List<ToolCallback> tools) {
        this.toolMap = new HashMap<>();
        if (tools != null) {
            for (ToolCallback tool : tools) {
                toolMap.put(tool.getToolDefinition().name(), tool);
            }
        }
    }

    /**
     * 执行单个工具调用。
     *
     * @param toolCall 工具调用信息
     * @param params   调用参数（本节暂不使用）
     * @return 工具执行结果文本
     */
    public String execute(AssistantMessage.ToolCall toolCall, RunnableParams params) {
        String toolName = toolCall.name();
        String argsJson = toolCall.arguments();

        if (argsJson == null || argsJson.isBlank()) {
            argsJson = "{}";
        }

        ToolCallback callback = toolMap.get(toolName);
        if (callback == null) {
            log.warn("Tool not found: {}", toolName);
            return "{\"error\": \"工具 '" + toolName + "' 不存在\"}";
        }

        try {
            log.debug("Executing tool: {} with args: {}", toolName, argsJson);
            ToolContext toolContext = new ToolContext(new HashMap<>());
            Object result = callback.call(argsJson, toolContext);
            return result != null ? result.toString() : "{}";
        } catch (Exception e) {
            log.error("Tool '{}' execution failed: {}", toolName, e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 构建工具响应消息。
     */
    public ToolResponseMessage buildToolResponseMessage(AssistantMessage.ToolCall toolCall, String result) {
        ToolResponseMessage.ToolResponse tr = new ToolResponseMessage.ToolResponse(
                toolCall.id(), toolCall.name(), result);
        return ToolResponseMessage.builder()
                .responses(List.of(tr))
                .build();
    }

    /**
     * 校验工具调用参数的 JSON 合法性。
     * 不合法时替换为空对象 {}。
     */
    public List<AssistantMessage.ToolCall> sanitizeToolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        List<AssistantMessage.ToolCall> fixed = new ArrayList<>(toolCalls.size());
        for (AssistantMessage.ToolCall tc : toolCalls) {
            String args = tc.arguments();
            if (args != null && !args.isBlank()) {
                try {
                    com.alibaba.fastjson2.JSON.parse(args);
                    fixed.add(tc);
                    continue;
                } catch (Exception e) {
                    log.warn("工具 '{}' 的 arguments 不是合法 JSON，已替换为空对象: {}", tc.name(), args);
                }
            }
            fixed.add(new AssistantMessage.ToolCall(tc.id(), tc.type(), tc.name(), "{}"));
        }
        return fixed;
    }
}
