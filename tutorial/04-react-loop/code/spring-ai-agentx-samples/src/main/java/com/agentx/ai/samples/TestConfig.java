package com.agentx.ai.samples;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 示例公共配置 — 从 secrets.properties 读取 API 配置，构造 ChatModel。
 * <p>
 * 首次运行前请将 secrets.properties.example 复制为 secrets.properties 并填入真实 Key。
 */
public final class TestConfig {

    private static final Properties secrets = loadSecrets();

    private static String s(String key) { return secrets.getProperty(key); }
    private static String s(String key, String def) { return secrets.getProperty(key, def); }

    private TestConfig() {
    }

    private static Properties loadSecrets() {
        Properties props = new Properties();
        try (InputStream is = TestConfig.class.getClassLoader().getResourceAsStream("secrets.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("[TestConfig] 未找到 secrets.properties，请复制 secrets.properties.example 并填入真实值");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load secrets.properties", e);
        }
        return props;
    }

    /**
     * 创建 OpenAI 兼容协议的 ChatModel（默认对接阿里云百炼 DashScope）。
     */
    public static ChatModel createChatModel() {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(s("dashscope.chat.model", "qwen-plus"))
                .temperature(0.7)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(s("dashscope.base.url", "https://dashscope.aliyuncs.com/compatible-mode/"))
                        .apiKey(s("dashscope.api.key"))
                        .build())
                .defaultOptions(options)
                .build();
    }
}
