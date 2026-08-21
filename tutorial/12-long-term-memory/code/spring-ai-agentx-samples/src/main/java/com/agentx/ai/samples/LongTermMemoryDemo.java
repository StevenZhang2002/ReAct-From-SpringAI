package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.memory.LongTermMemoryConfig;
import com.agentx.ai.core.memory.LongTermMemoryManager;
import com.agentx.ai.core.model.RunnableParams;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * 第 12 节示例：分层记忆体系 — 长期记忆跨会话持久，按语义检索注入。
 * <p>
 * 使用内存版 SimpleVectorStore（无需数据库），演示记忆的存储与检索。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.LongTermMemoryDemo
 */
public class LongTermMemoryDemo {

    public static void main(String[] args) throws InterruptedException {
        ChatModel chatModel = TestConfig.createChatModel();
        EmbeddingModel embeddingModel = TestConfig.createEmbeddingModel();

        // 1. 构建长期记忆配置：内存向量库 + 语义检索
        VectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        LongTermMemoryConfig memoryConfig = LongTermMemoryConfig.builder()
                .vectorStore(vectorStore)
                .topK(3)
                .similarityThreshold(0.3)
                .build();

        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .longTermMemory(memoryConfig)
                .maxRounds(3)
                .build();

        String userId = "user-ltm-demo";

        // 2. 手动注入一条长期记忆（模拟跨会话沉淀的事实）
        LongTermMemoryManager manager = new LongTermMemoryManager(memoryConfig);
        manager.storeMemory(userId, "用户小明是一名 Java 后端开发者，正在构建基于 Spring AI 的智能客服机器人");

        System.out.println(">>> 已注入长期记忆，等待向量入库...");
        Thread.sleep(5000);

        // 3. 新会话（无对话历史）提问，验证长期记忆被检索注入
        RunnableParams params = RunnableParams.builder()
                .conversationId("conv-ltm-2")
                .userId(userId)
                .build();

        System.out.println(">>> 用户：你知道我的职业吗？我最近在做什么？");
        String answer = agent.call("你知道我的职业吗？我最近在做什么？", params);
        System.out.println("<<< Agent：" + answer);
    }
}
