package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.context.ContextPolicy;
import com.agentx.ai.core.model.RunnableParams;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 第 11 节示例：上下文压缩 — 长对话自动触发历史消息压缩，防止 Token 爆炸。
 * <p>
 * 演示：调低压缩阈值（消息数 > 8 即压缩），多轮对话观察压缩触发。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.ContextCompressionDemo
 */
public class ContextCompressionDemo {

    public static void main(String[] args) {
        ChatModel chatModel = TestConfig.createChatModel();

        // 1. 自定义压缩策略：消息数超过 8 条或 token 超过 6000 即压缩
        ContextPolicy policy = ContextPolicy.builder()
                .msgThreshold(8)
                .tokenThreshold(6000)
                .build();

        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .contextPolicy(policy)
                .maxRounds(3)
                .build();

        // 2. 同一会话连续多轮对话，观察日志中的压缩触发
        RunnableParams params = RunnableParams.builder()
                .conversationId("conv-compress-demo")
                .userId("user-001")
                .build();

        String[] turns = {
                "我叫小明，是 Java 后端开发者",
                "我最近在学习 Spring AI，想做一个智能客服机器人",
                "这个机器人需要支持多轮对话、工具调用和知识库检索",
                "我计划用 DeepSeek 作为底层模型，你觉得合适吗？",
                "除了 DeepSeek，还有哪些适合中文场景的模型推荐？",
                "如果要做流式输出，Spring AI 有什么现成方案？",
                "最后帮我总结一下我提到的所有需求"
        };

        for (int i = 0; i < turns.length; i++) {
            System.out.println(">>> 第 " + (i + 1) + " 轮：" + turns[i]);
            String answer = agent.call(turns[i], params);
            System.out.println("<<< Agent：" + (answer.length() > 80 ? answer.substring(0, 80) + "..." : answer));
        }
    }
}
