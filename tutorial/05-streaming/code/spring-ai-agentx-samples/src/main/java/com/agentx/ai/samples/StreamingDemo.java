package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.model.AgentStreamEvent;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 第 05 节示例：流式输出 — 逐 token 打印与结构化流事件。
 * <p>
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.StreamingDemo
 */
public class StreamingDemo {

    public static void main(String[] args) {
        ChatModel chatModel = TestConfig.createChatModel();

        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .maxRounds(3)
                .build();

        // 1. 流式文本：Flux<String> 逐段输出
        System.out.println("========== 流式文本输出 ==========");
        String query = "用三句话介绍一下 Spring AI 是什么";
        System.out.println(">>> 用户：" + query);
        agent.stream(query)
                .doOnNext(chunk -> System.out.print(chunk))
                .blockLast();
        System.out.println("\n========== 输出结束 ==========");

        // 2. 结构化流事件：观察每轮事件类型（Text / ToolStart / ToolEnd / Complete）
        System.out.println("\n========== 结构化流事件 ==========");
        agent.streamForResult("1+1 等于几？", com.agentx.ai.core.model.RunnableParams.empty())
                .doOnNext(StreamingDemo::printEvent)
                .blockLast();
    }

    /** 按事件类型打印 */
    private static void printEvent(AgentStreamEvent event) {
        switch (event) {
            case AgentStreamEvent.Text t ->
                    System.out.println("[Text] " + t.content());
            case AgentStreamEvent.ToolStart s ->
                    System.out.println("[ToolStart] 工具：" + s.toolName() + "，参数：" + s.arguments());
            case AgentStreamEvent.ToolEnd e ->
                    System.out.println("[ToolEnd] 工具：" + e.toolName() + "，结果：" + e.result());
            case AgentStreamEvent.Complete c ->
                    System.out.println("[Complete] prompt=" + c.totalPromptTokens()
                            + "，completion=" + c.totalCompletionTokens());
            case AgentStreamEvent.Error err ->
                    System.out.println("[Error] " + err.message());
        }
    }
}
