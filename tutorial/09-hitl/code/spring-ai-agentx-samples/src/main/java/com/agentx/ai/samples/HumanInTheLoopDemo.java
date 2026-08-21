package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.interrupt.PauseReason;
import com.agentx.ai.core.model.AgentResult;
import com.agentx.ai.core.model.PauseState;
import com.agentx.ai.core.model.RunnableParams;
import org.springframework.ai.chat.model.ChatModel;

import java.util.HashMap;
import java.util.Map;

/**
 * 第 09 节示例：Human-in-the-Loop — Agent 遇到需要用户决策的问题时暂停等待。
 * <p>
 * 演示：1) 开启 askUser 后，Agent 调用 ask_user 工具会被拦截并暂停；
 * 2) 拿到 PauseState 后由用户回答，resume 恢复执行。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.HumanInTheLoopDemo
 */
public class HumanInTheLoopDemo {

    public static void main(String[] args) {
        ChatModel chatModel = TestConfig.createChatModel();

        // 1. 开启 Human-in-the-Loop
        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .askUser(true)
                .maxRounds(5)
                .build();

        // 2. 提问一个需要用户决策的问题
        String query = "我要出门旅行，帮我选一个目的地：A. 去杭州看西湖  B. 去成都吃火锅  C. 去拉萨看雪山。请问我该选哪个？";
        System.out.println(">>> 用户：" + query);

        AgentResult result = agent.callForResult(query, RunnableParams.empty());

        // 3. 若暂停，由用户回答后恢复
        if (result instanceof AgentResult.Paused paused) {
            PauseState state = paused.state();
            System.out.println("=== Agent 暂停，等待用户决策 ===");
            System.out.println("暂停原因：" + state.getReason());
            state.getPendingToolCalls().forEach(ptc ->
                    System.out.println("待回答问题 [" + ptc.id() + "]：" + ptc.arguments()));

            // 模拟用户选择「成都吃火锅」
            Map<String, String> answers = new HashMap<>();
            state.getPendingToolCalls().forEach(ptc -> answers.put(ptc.id(), "我选择 B，成都吃火锅"));

            System.out.println(">>> 用户回答：我选择 B，成都吃火锅");
            AgentResult resumed = agent.resume(state, answers);
            if (resumed instanceof AgentResult.Completed completed) {
                System.out.println("<<< Agent：" + completed.answer());
            }
        } else if (result instanceof AgentResult.Completed completed) {
            System.out.println("<<< Agent（未暂停）：" + completed.answer());
        }
    }
}
