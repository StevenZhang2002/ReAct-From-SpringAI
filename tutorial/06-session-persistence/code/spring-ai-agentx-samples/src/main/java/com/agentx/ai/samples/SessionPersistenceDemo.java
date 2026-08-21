package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.model.RunnableParams;
import org.springframework.ai.chat.model.ChatModel;

import javax.sql.DataSource;

/**
 * 第 06 节示例：会话持久化 — 多轮对话自动落库，下次调用自动加载历史。
 * <p>
 * 使用 H2 内存数据库（MySQL 兼容模式），无需额外安装。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.SessionPersistenceDemo
 */
public class SessionPersistenceDemo {

    public static void main(String[] args) {
        ChatModel chatModel = TestConfig.createChatModel();
        DataSource dataSource = TestConfig.createH2DataSource();

        // 1. 构建 Agent：启用会话持久化
        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .dataSource(dataSource)
                .enableSession(true)
                .maxRounds(3)
                .build();

        // 2. 同一会话 ID 下第一轮对话：注入事实
        RunnableParams params = RunnableParams.builder()
                .conversationId("conv-session-demo")
                .userId("user-001")
                .build();

        System.out.println(">>> 第一轮：我叫小明，我的爱好是写代码");
        String answer1 = agent.call("我叫小明，我的爱好是写代码", params);
        System.out.println("<<< Agent：" + answer1);

        // 3. 同一会话 ID 下第二轮对话：验证 Agent 记得历史
        System.out.println("\n>>> 第二轮：我叫什么名字？我的爱好是什么？");
        String answer2 = agent.call("我叫什么名字？我的爱好是什么？", params);
        System.out.println("<<< Agent：" + answer2);

        // 4. 新会话 ID 下提问同样的问题：验证「失忆」对比
        RunnableParams newParams = RunnableParams.builder()
                .conversationId("conv-session-demo-2")
                .userId("user-001")
                .build();
        System.out.println("\n>>> 新会话（无历史）：我叫什么名字？");
        String answer3 = agent.call("我叫什么名字？", newParams);
        System.out.println("<<< Agent：" + answer3);
    }
}
