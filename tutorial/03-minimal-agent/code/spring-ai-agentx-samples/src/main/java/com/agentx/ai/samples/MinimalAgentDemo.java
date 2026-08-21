package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.model.AgentResult;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 第 03 节示例：最小 Agent — 一行代码构造，一次调用对话。
 * <p>
 * 运行前请先配置 secrets.properties（复制 secrets.properties.example 并填入 API Key）。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.MinimalAgentDemo
 */
public class MinimalAgentDemo {

    public static void main(String[] args) {
        // 1. 创建 ChatModel（OpenAI 兼容协议，默认对接阿里云百炼）
        ChatModel chatModel = TestConfig.createChatModel();

        // 2. 构建 Agent：只需要 chatModel，其余全部走默认值
        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .build();

        // 3. 调用一次对话
        System.out.println(">>> 用户：你好，请用一句话介绍你自己");
        String answer = agent.call("你好，请用一句话介绍你自己");
        System.out.println("<<< Agent：" + answer);

        // 4. 结构化结果：AgentResult 包含 answer 与 token 统计
        AgentResult result = agent.callForResult("1+1 等于多少？", com.agentx.ai.core.model.RunnableParams.empty());
        if (result instanceof AgentResult.Completed completed) {
            System.out.println("<<< Agent：" + completed.answer());
        }
    }
}
