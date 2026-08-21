package com.agentx.ai.samples;

import com.agentx.ai.core.agent.ReactAgent;
import com.agentx.ai.core.model.RunnableParams;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 第 13 节示例：追踪审计 — 每轮 ReAct 循环自动记录输入/输出/耗时/Token。
 * <p>
 * 演示：开启 enableTrace 后执行对话，再从 agentx_trace 表查询审计记录。
 * 运行：mvn -pl spring-ai-agentx-samples exec:java -Dexec.mainClass=com.agentx.ai.samples.TraceAuditDemo
 */
public class TraceAuditDemo {

    public static void main(String[] args) {
        ChatModel chatModel = TestConfig.createChatModel();
        DataSource dataSource = TestConfig.createH2DataSource();

        // 1. 构建 Agent：开启追踪审计
        ReactAgent agent = ReactAgent.builder()
                .chatModel(chatModel)
                .dataSource(dataSource)
                .enableTrace(true)
                .maxRounds(3)
                .build();

        // 2. 执行两轮对话
        RunnableParams params = RunnableParams.builder()
                .conversationId("conv-trace-demo")
                .userId("user-001")
                .build();

        System.out.println(">>> 用户：用一句话介绍 Spring AI");
        String answer1 = agent.call("用一句话介绍 Spring AI", params);
        System.out.println("<<< Agent：" + answer1);

        System.out.println("\n>>> 用户：再介绍一下 Agent 是什么");
        String answer2 = agent.call("再介绍一下 Agent 是什么", params);
        System.out.println("<<< Agent：" + answer2);

        // 3. 查询追踪记录
        System.out.println("\n========== agentx_trace 追踪记录 ==========");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.query("SELECT session_id, round, input_data, output_data, prompt_tokens, "
                        + "completion_tokens, duration_ms, success FROM agentx_trace ORDER BY id",
                rs -> {
                    System.out.println("session_id=" + rs.getLong("session_id")
                            + " | round=" + rs.getInt("round")
                            + " | prompt=" + rs.getInt("prompt_tokens")
                            + " | completion=" + rs.getInt("completion_tokens")
                            + " | 耗时=" + rs.getLong("duration_ms") + "ms"
                            + " | success=" + rs.getInt("success"));
                    System.out.println("    输入: " + truncate(rs.getString("input_data")));
                    System.out.println("    输出: " + truncate(rs.getString("output_data")));
                });
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }
}
