package com.agentx.ai.core.tools.toolsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 工具搜索元工具 — 让 LLM 按需搜索并加载 deferred 工具。
 *
 * @author bigchui
 */
public class ToolSearchTool {

    private static final Logger log = LoggerFactory.getLogger(ToolSearchTool.class);

    /**
     * 创建 ToolSearchTool 的 ToolCallback。
     *
     * @param config          搜索配置
     * @param deferredPool    延迟工具池（name → ToolCallback）
     * @param discoveredNames 已发现工具名称集合（共享可变集合）
     * @return ToolCallback
     */
    public static ToolCallback create(ToolSearchConfig config,
                                      Map<String, ToolCallback> deferredPool,
                                      Set<String> discoveredNames) {
        ToolSearchFunction fn = new ToolSearchFunction(config, deferredPool, discoveredNames);

        return FunctionToolCallback.builder("tool_search", fn)
                .description("搜索可用的工具。当当前工具不足以完成用户任务时，调用此工具搜索更多工具。")
                .inputType(ToolSearchFunction.SearchRequest.class)
                .build();
    }

    // ==================== 关键词搜索 ====================

    static List<String> keywordSearch(Map<String, ToolCallback> deferredPool, String query, int maxResults) {
        String lowerQuery = query.toLowerCase();
        List<String> found = new ArrayList<>();

        for (Map.Entry<String, ToolCallback> entry : deferredPool.entrySet()) {
            String name = entry.getKey();
            ToolCallback tool = entry.getValue();
            String description = tool.getToolDefinition().description();

            // 简单的关键词匹配
            if (name.toLowerCase().contains(lowerQuery) ||
                    (description != null && description.toLowerCase().contains(lowerQuery))) {
                found.add(name);
                if (found.size() >= maxResults) {
                    break;
                }
            }
        }

        return found;
    }

    // ==================== Function 实现 ====================

    static class ToolSearchFunction implements Function<ToolSearchFunction.SearchRequest, String> {

        private final ToolSearchConfig config;
        private final Map<String, ToolCallback> deferredPool;
        private final Set<String> discoveredNames;

        ToolSearchFunction(ToolSearchConfig config, Map<String, ToolCallback> deferredPool,
                           Set<String> discoveredNames) {
            this.config = config;
            this.deferredPool = deferredPool;
            this.discoveredNames = discoveredNames;
        }

        @Override
        public String apply(SearchRequest request) {
            String query = request.query();
            log.debug("tool_search called with query: {}", query);

            List<String> foundNames = keywordSearch(deferredPool, query, config.maxResults());

            if (foundNames.isEmpty()) {
                return "未找到相关工具。请尝试使用不同的关键词，或者用其他方式描述你的需求。";
            }

            // 标记为已发现
            discoveredNames.addAll(foundNames);

            // 构建返回结果
            StringBuilder sb = new StringBuilder("找到以下工具：\n");
            for (String name : foundNames) {
                ToolCallback tool = deferredPool.get(name);
                if (tool != null) {
                    String desc = tool.getToolDefinition().description();
                    sb.append("- ").append(name);
                    if (desc != null && !desc.isEmpty()) {
                        String shortDesc = desc.length() > 100 ? desc.substring(0, 100) + "..." : desc;
                        sb.append(": ").append(shortDesc);
                    }
                    sb.append("\n");
                }
            }
            sb.append("这些工具已加载到当前会话，你可以在下一轮直接调用。");
            return sb.toString();
        }

        record SearchRequest(
                @ToolParam(description = "搜索关键词或描述，用于匹配可用的工具") String query
        ) {}
    }
}
