package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.model.AgentStreamEvent;
import com.agentx.ai.core.model.RunnableParams;
import com.agentx.ai.core.model.ThinkingMode;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 第 08 节示例：思考模型适配 — 观察模型的推理过程（think 内容）。
 * <p>
 * 适用于 DeepSeek、Qwen3.6 等通过 reasoning_content 返回思考的模型。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.ThinkingModeDemo
 */
public class ThinkingModeDemo {

    public static void main(String[] args) {
        ChatModel chatModel = TestConfig.createChatModel();

        // 1. 启用思考模式：REASONING_CONTENT 适配 DeepSeek / Qwen 思考模型
        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .thinkingMode(ThinkingMode.REASONING_CONTENT)
                .maxRounds(3)
                .build();

        System.out.println("思考模式：" + agent.getThinkingMode());

        // 2. 提一个需要推理的问题，观察 think 与回答的分离
        String query = "一个农夫带着一只狼、一只羊和一筐白菜过河，船一次只能载农夫和一样东西，"
                + "狼会吃羊、羊会吃白菜，农夫怎样才能把三样东西都安全带过河？";
        System.out.println(">>> 用户：" + query);

        agent.streamForResult(query, RunnableParams.empty())
                .doOnNext(ThinkingModeDemo::printEvent)
                .blockLast();
    }

    /** 按事件类型打印（think 内容单独标记） */
    private static void printEvent(AgentStreamEvent event) {
        switch (event) {
            case AgentStreamEvent.Text t -> System.out.print(t.content());
            case AgentStreamEvent.Complete c -> System.out.println("\n[Complete] prompt=" + c.totalPromptTokens()
                    + "，completion=" + c.totalCompletionTokens());
            case AgentStreamEvent.Error err -> System.out.println("\n[Error] " + err.message());
            default -> { /* 工具事件忽略 */ }
        }
    }
}
