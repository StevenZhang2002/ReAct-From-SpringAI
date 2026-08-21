package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.tools.TodoWriteTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;

/**
 * 第 14 节示例：进阶工具 — TodoWrite 任务追踪 + 自定义业务工具组合。
 * <p>
 * 演示：注册 TodoWriteTool（任务清单）+ 一个天气预报工具，Agent 自主拆解任务并执行。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.AdvancedToolsDemo
 */
public class AdvancedToolsDemo {

    /** 自定义业务工具：查询城市天气（模拟数据） */
    @Tool(description = "查询指定城市的天气情况")
    public String getWeather(@org.springframework.ai.tool.annotation.ToolParam(description = "城市名称，如：北京") String city) {
        return city + "：晴，25°C，空气质量优，适合出行。";
    }

    public static void main(String[] args) {
        ChatModel chatModel = TestConfig.createChatModel();

        // 1. 组合工具：TodoWrite（任务追踪）+ 自定义天气工具
        ToolCallback[] todoTools = TodoWriteTool.create();
        ToolCallback[] bizTools = ToolCallbacks.from(new AdvancedToolsDemo());
        ToolCallback[] allTools = new ToolCallback[todoTools.length + bizTools.length];
        System.arraycopy(todoTools, 0, allTools, 0, todoTools.length);
        System.arraycopy(bizTools, 0, allTools, todoTools.length, bizTools.length);

        // 2. 构建 Agent：注册全部工具
        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .tools(allTools)
                .maxRounds(8)
                .build();

        // 3. 提问一个需要「拆解任务 + 查询信息」的复合问题
        String query = "我准备周末去北京旅游，请帮我："
                + "1) 用 TodoWrite 规划一个三天的行程任务清单 "
                + "2) 查询一下北京的天气，给出穿衣建议";
        System.out.println(">>> 用户：" + query);

        String answer = agent.call(query);
        System.out.println("<<< Agent：" + answer);
    }
}
