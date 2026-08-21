package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.model.RunnableParams;
import org.springframework.ai.chat.model.ChatModel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 第 07 节示例：动态会话参数 + 并发控制。
 * <p>
 * 演示：1) 通过 RunnableParams 传入自定义参数，Prompt 中自动注入；
 * 2) 两个会话并发执行，任务管理器跟踪运行状态。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.TaskManagerDemo
 */
public class TaskManagerDemo {

    public static void main(String[] args) throws InterruptedException {
        ChatModel chatModel = TestConfig.createChatModel();

        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .maxRounds(3)
                .build();

        // 1. 自定义参数：给不同用户不同语气
        RunnableParams paramsA = RunnableParams.builder()
                .conversationId("conv-a")
                .userId("user-a")
                .addParam("nickname", "阿明")
                .build();

        RunnableParams paramsB = RunnableParams.builder()
                .conversationId("conv-b")
                .userId("user-b")
                .addParam("nickname", "小美")
                .build();

        // 2. 并发执行两个任务
        AtomicInteger finished = new AtomicInteger();
        new Thread(() -> {
            String r = agent.call("请用你的语气打个招呼，并简单介绍你自己", paramsA);
            System.out.println("<<< 会话A：" + r);
            finished.incrementAndGet();
        }, "task-a").start();

        new Thread(() -> {
            String r = agent.call("请用你的语气打个招呼，并简单介绍你自己", paramsB);
            System.out.println("<<< 会话B：" + r);
            finished.incrementAndGet();
        }, "task-b").start();

        // 3. 轮询任务管理器，观察并发状态
        while (finished.get() < 2) {
            System.out.println("[任务管理器] 当前并发任务数：" + agent.getTaskManager().getTaskCount());
            Thread.sleep(2000);
        }
        System.out.println("[任务管理器] 全部任务完成，剩余任务数：" + agent.getTaskManager().getTaskCount());
    }
}
