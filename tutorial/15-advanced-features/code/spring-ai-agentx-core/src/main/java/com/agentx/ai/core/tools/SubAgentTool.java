package com.agentx.ai.core.tools;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.RunnableParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * SubAgent 工具 — 将 ReactAgent 包装为 ToolCallback。
 * <p>
 * 核心职责：
 * <ul>
 *   <li>接收主 Agent LLM 的委派请求</li>
 *   <li>通过 Supplier 创建独立的子 ReactAgent 实例</li>
 *   <li>透传 RunnableParams，保持调用上下文一致</li>
 * </ul>
 *
 * @author bigchui
 */
public class SubAgentTool {

    private static final Logger log = LoggerFactory.getLogger(SubAgentTool.class);

    /**
     * 创建 SubAgentTool 的 ToolCallback。
     *
     * @param agentName     子 Agent 名称（工具名称自动生成为 call_{name}）
     * @param description   工具描述（LLM 根据此描述决定是否委派）
     * @param agentProvider 子 Agent 工厂（每次调用创建新实例）
     * @return ToolCallback
     */
    public static ToolCallback create(String agentName, String description,
                                       Supplier<ReactAgent> agentProvider) {
        String toolName = "call_" + agentName;
        String toolDescription = description + "。将任务委派给此子Agent处理，它将返回处理结果。";

        Function<SubAgentRequest, String> fn = request -> execute(
                agentName, agentProvider, request.message());

        return FunctionToolCallback.builder(toolName, fn)
                .description(toolDescription)
                .inputType(SubAgentRequest.class)
                .build();
    }

    /**
     * 执行子 Agent。
     */
    private static String execute(String agentName, Supplier<ReactAgent> agentProvider,
                                   String message) {
        ReactAgent subAgent = agentProvider.get();

        log.debug("SubAgent[{}] execution started", agentName);

        RunnableParams params = RunnableParams.empty();
        AgentResult agentResult = subAgent.callForResult(message, params);

        if (agentResult instanceof AgentResult.Completed c) {
            log.debug("SubAgent[{}] execution completed", agentName);
            return c.answer();
        }
        if (agentResult instanceof AgentResult.Failed f) {
            return "SubAgent 执行失败：" + f.error();
        }
        return "SubAgent 执行异常：未知结果类型";
    }

    // ==================== 请求参数 ====================

    /**
     * SubAgent 工具的输入参数。
     */
    public record SubAgentRequest(
            String message
    ) {}
}
