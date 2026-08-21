package com.agentx.ai.core.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Scanner;

/**
 * 问用户工具 — 让 Agent 能主动向用户提问。
 * <p>
 * 两种模式：
 * <ul>
 *   <li>HITL 模式：配合 askUser=true，工具被拦截不执行，问题带出给调用方</li>
 *   <li>CLI 模式：{@link #createBlocking()} 使用 Scanner 阻塞等待控制台输入</li>
 * </ul>
 *
 * @author bigchui
 */
public class AskUserTool {

    @Tool(name = "ask_user", description = """
            向用户提问。在给出任何计划、方案、推荐、建议之前，必须先调用此工具了解用户的具体情况和偏好。
            
            参数使用：
            - question：必填，清晰描述要问用户的问题
            - options：可选，供用户选择的选项列表
            """)
    public String askUser(
            @ToolParam(description = "要问用户的问题") String question,
            @ToolParam(description = "供用户选择的选项列表", required = false) List<String> options) {
        return "此工具需要配合 askUser=true 使用，或使用 createBlocking() 创建 CLI 版本";
    }

    /** HITL 模式 */
    public static ToolCallback[] create() {
        return ToolCallbacks.from(new AskUserTool());
    }

    /** CLI 模式：阻塞等待控制台输入 */
    public static ToolCallback[] createBlocking() {
        return ToolCallbacks.from(new BlockingAskUserTool());
    }

    static class BlockingAskUserTool {
        private final Scanner scanner = new Scanner(System.in);

        @Tool(name = "ask_user", description = """
                向用户提问。在给出任何计划、方案、推荐、建议之前，必须先调用此工具了解用户偏好。
                """)
        public String askUser(
                @ToolParam(description = "要问用户的问题") String question,
                @ToolParam(description = "供用户选择的选项列表", required = false) List<String> options) {
            System.out.println("\n=== AI 提问 ===");
            System.out.println(question);
            if (options != null && !options.isEmpty()) {
                System.out.println("选项：");
                for (int i = 0; i < options.size(); i++) {
                    System.out.println("  " + (i + 1) + ". " + options.get(i));
                }
            }
            System.out.print("请输入回答: ");
            String answer = scanner.nextLine();
            System.out.println("==============\n");
            return answer;
        }
    }
}
