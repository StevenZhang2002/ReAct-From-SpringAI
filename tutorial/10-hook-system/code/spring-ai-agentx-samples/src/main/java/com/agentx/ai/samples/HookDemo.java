package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.hook.AfterCallEvent;
import com.agentx.ai.core.hook.AfterToolExecutionEvent;
import com.agentx.ai.core.hook.AgentHook;
import com.agentx.ai.core.hook.BeforeCallEvent;
import com.agentx.ai.core.hook.BeforeToolExecutionEvent;
import com.agentx.ai.core.model.RunnableParams;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 第 10 节示例：Hook 机制 — 在 Agent 生命周期关键节点插入自定义逻辑。
 * <p>
 * 演示：注册一个日志 Hook，观察 beforeCall / afterCall / beforeTool 事件的触发。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.HookDemo
 */
public class HookDemo {

    public static void main(String[] args) {
        ChatModel chatModel = TestConfig.createChatModel();

        // 1. 自定义 Hook：在关键节点打印日志
        AgentHook loggingHook = new AgentHook() {
            @Override
            public int priority() {
                return 0;
            }

            @Override
            public void beforeCall(BeforeCallEvent event) {
                System.out.println("[Hook:beforeCall] 即将调用 LLM，用户问题：" + event.getQuery());
            }

            @Override
            public void afterCall(AfterCallEvent event) {
                System.out.println("[Hook:afterCall] LLM 调用完成，耗时：" + event.getDurationMs() + "ms");
            }

            @Override
            public void beforeToolExecution(BeforeToolExecutionEvent event) {
                System.out.println("[Hook:beforeTool] 即将执行工具：" + event.getToolName()
                        + "，参数：" + event.getArguments());
            }

            @Override
            public void afterToolExecution(AfterToolExecutionEvent event) {
                System.out.println("[Hook:afterTool] 工具 " + event.getToolName() + " 执行完成，结果：" + event.getResult());
            }
        };

        // 2. 注册 Hook 并运行
        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .hooks(loggingHook)
                .maxRounds(3)
                .build();

        System.out.println(">>> 用户：1+1 等于几？");
        String answer = agent.call("1+1 等于几？", RunnableParams.empty());
        System.out.println("<<< Agent：" + answer);
    }
}
