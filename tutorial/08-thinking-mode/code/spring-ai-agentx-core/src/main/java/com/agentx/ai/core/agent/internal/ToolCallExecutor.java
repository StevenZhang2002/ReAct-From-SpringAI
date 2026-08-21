package com.agentx.ai.core.agent.internal;

import com.agentx.ai.core.model.RunnableParams;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工具调用执行器 — 负责工具执行、参数替换和结果消息组装。
 * <p>
 * 本节新增：replaceToolParams() — 按工具 inputSchema 过滤注入 toolParams，
 * 确保 userId/token 等运行时参数安全注入，不依赖 LLM 生成。
 *
 * @author bigchui
 */
public class ToolCallExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolCallExecutor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
     * @param params   调用参数（toolParams 注入）
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

        // 本节新增：按 inputSchema 过滤注入 toolParams
        argsJson = replaceToolParams(callback, argsJson, params);

        try {
            log.debug("Executing tool: {} with args: {}", toolName, argsJson);
            ToolContext toolContext = buildToolContext(params);
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

    // ==================== toolParams 替换 ====================

    /**
     * 按工具的 inputSchema 过滤注入 toolParams。
     * <p>
     * 只注入该工具真实声明的字段（避免 MCP 服务端严格校验报错）。
     *
     * @param callback 工具回调
     * @param argsJson LLM 生成的参数 JSON
     * @param params   调用参数（含 toolParams）
     * @return 注入后的参数 JSON
     */
    private String replaceToolParams(ToolCallback callback, String argsJson, RunnableParams params) {
        if (params == null || params.getToolParams() == null || params.getToolParams().isEmpty()) {
            return argsJson;
        }
        if (argsJson == null || argsJson.isBlank()) {
            return argsJson;
        }

        Set<String> accepted = getAcceptedParamNames(callback);
        if (accepted.isEmpty()) {
            log.debug("工具 {} 的 inputSchema 无 properties，跳过 toolParams 注入",
                    callback.getToolDefinition().name());
            return argsJson;
        }

        try {
            Map<String, Object> args = objectMapper.readValue(argsJson,
                    new TypeReference<Map<String, Object>>() {});
            for (Map.Entry<String, Object> entry : params.getToolParams().entrySet()) {
                if (accepted.contains(entry.getKey())) {
                    args.put(entry.getKey(), entry.getValue());
                } else {
                    log.debug("工具 {} 不接受参数 {}，跳过注入",
                            callback.getToolDefinition().name(), entry.getKey());
                }
            }
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            log.error("替换工具参数失败（argsJson 不是合法 JSON，原样返回）: {}", argsJson, e);
            return argsJson;
        }
    }

    /**
     * 从 ToolCallback 的 inputSchema 提取合法参数名集合。
     * schema 形如 {"type":"object","properties":{"sql":{...},"userId":{...}}}。
     */
    private Set<String> getAcceptedParamNames(ToolCallback callback) {
        try {
            String schemaJson = callback.getToolDefinition().inputSchema();
            if (schemaJson == null || schemaJson.isBlank()) {
                return Set.of();
            }
            Map<String, Object> schema = objectMapper.readValue(schemaJson,
                    new TypeReference<Map<String, Object>>() {});
            Object properties = schema.get("properties");
            if (!(properties instanceof Map<?, ?> map)) {
                return Set.of();
            }
            Set<String> names = new HashSet<>(map.size());
            for (Object k : map.keySet()) {
                if (k != null) {
                    names.add(k.toString());
                }
            }
            return names;
        } catch (Exception e) {
            log.warn("解析工具 {} inputSchema 失败，跳过 toolParams 注入: {}",
                    callback.getToolDefinition().name(), e.getMessage());
            return Set.of();
        }
    }

    /**
     * 构建 ToolContext（注入 userId/conversationId 等运行时信息）。
     */
    private ToolContext buildToolContext(RunnableParams params) {
        Map<String, Object> context = new HashMap<>();
        if (params != null) {
            if (params.getUserId() != null) {
                context.put("userId", params.getUserId());
            }
            if (params.getConversationId() != null) {
                context.put("conversationId", params.getConversationId());
            }
            context.put("runnableParams", params);
        }
        return new ToolContext(context);
    }

    // ==================== 工具调用校验 ====================

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
