package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 第 04 节示例：ReAct 循环 — Agent 自动调用工具完成多步任务。
 * <p>
 * 本示例注册了两个本地工具（获取当前时间、加法），Agent 会自主决定何时调用。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.ToolCallDemo
 */
public class ToolCallDemo {

    /** 本地工具 1：获取当前时间 */
    @Tool(description = "获取当前系统时间")
    public String getCurrentTime() {
        return java.time.LocalDateTime.now().toString();
    }

    /** 本地工具 2：整数加法 */
    @Tool(description = "计算两个整数的和")
    public int add(@ToolParam(description = "第一个加数") int a,
                   @ToolParam(description = "第二个加数") int b) {
        return a + b;
    }

    public static void main(String[] args) {
        ChatModel chatModel = TestConfig.createChatModel();

        // 1. 将 @Tool 方法注册为 ToolCallback
        ToolCallDemo demo = new ToolCallDemo();
        ToolCallback[] tools = ToolCallbacks.from(demo);

        // 2. 构建 Agent：注册工具，最多 5 轮 ReAct 循环
        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .tools(tools)
                .maxRounds(5)
                .build();

        // 3. 提问一个需要多步推理 + 工具调用的问题
        String query = "现在是几点？另外帮我计算 23 + 45 等于多少？";
        System.out.println(">>> 用户：" + query);

        String answer = agent.call(query);
        System.out.println("<<< Agent：" + answer);
    }
}
