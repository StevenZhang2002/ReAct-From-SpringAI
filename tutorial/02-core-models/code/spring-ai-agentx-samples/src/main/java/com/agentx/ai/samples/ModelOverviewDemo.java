package com.agentx.ai.samples;

import com.agentx.ai.core.exception.AgentErrorCode;
import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.AgentStreamEvent;
import com.agentx.ai.core.model.OutputType;
import com.agentx.ai.core.model.RunnableParams;
import com.agentx.ai.core.model.ThinkingMode;

/**
 * 第 02 节示例：核心模型 API 一览。
 * <p>
 * 本示例不需要 API Key，纯本地演示核心数据模型的使用方式。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.ModelOverviewDemo
 */
public class ModelOverviewDemo {

    public static void main(String[] args) {
        System.out.println("========== AgentResult：Agent 执行结果 ==========");
        AgentResult completed = new AgentResult.Completed("你好，我是 Agent");
        System.out.println("Completed 类型：" + completed.getClass().getSimpleName()
                + "，答案：" + ((AgentResult.Completed) completed).answer());

        AgentResult failed = new AgentResult.Failed("API 调用超时", AgentErrorCode.LLM_CALL_FAILED);
        System.out.println("Failed 类型：" + failed.getClass().getSimpleName()
                + "，原因：" + ((AgentResult.Failed) failed).error()
                + "，错误码：" + ((AgentResult.Failed) failed).code());

        System.out.println("\n========== AgentStreamEvent：流式事件 ==========");
        AgentStreamEvent text = new AgentStreamEvent.Text("你好");
        AgentStreamEvent toolStart = new AgentStreamEvent.ToolStart("getCurrentTime", "call_1", "{}");
        AgentStreamEvent toolEnd = new AgentStreamEvent.ToolEnd("getCurrentTime", "call_1", "2026-08-22 10:00:00");
        AgentStreamEvent complete = new AgentStreamEvent.Complete(1200, 300);
        System.out.println("Text：" + ((AgentStreamEvent.Text) text).content());
        System.out.println("ToolStart：" + ((AgentStreamEvent.ToolStart) toolStart).toolName());
        System.out.println("ToolEnd 结果：" + ((AgentStreamEvent.ToolEnd) toolEnd).result());
        System.out.println("Complete token：" + ((AgentStreamEvent.Complete) complete).totalPromptTokens()
                + " + " + ((AgentStreamEvent.Complete) complete).totalCompletionTokens());

        System.out.println("\n========== RunnableParams：运行时参数 ==========");
        RunnableParams params = RunnableParams.builder()
                .conversationId("conv-001")
                .userId("user-001")
                .addParam("theme", "编程")
                .outputType(OutputType.of(String.class))
                .build();
        System.out.println("conversationId：" + params.getConversationId());
        System.out.println("userId：" + params.getUserId());
        System.out.println("自定义参数 theme：" + params.getParam("theme", "默认值"));
        System.out.println("输出类型：" + params.getOutputType().getRawType());

        System.out.println("\n========== 枚举 ==========");
        System.out.println("ThinkingMode 取值：" + java.util.Arrays.toString(ThinkingMode.values()));
    }
}
