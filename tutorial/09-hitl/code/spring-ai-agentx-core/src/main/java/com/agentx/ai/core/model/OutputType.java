package com.agentx.ai.core.model;

import org.springframework.core.ParameterizedTypeReference;

/**
 * 结构化输出类型描述。
 * <p>
 * 封装 {@link ParameterizedTypeReference}，用于 Agent 结构化输出场景。
 * 当设置了 outputType 后，框架会在用户消息中追加 JSON Schema 格式要求，
 * 并在收到响应后尝试修复 JSON。
 *
 * @author bigchui
 */
public class OutputType {

    private final ParameterizedTypeReference<?> typeReference;
    private final Class<?> rawType;

    private OutputType(ParameterizedTypeReference<?> typeReference, Class<?> rawType) {
        this.typeReference = typeReference;
        this.rawType = rawType;
    }

    /**
     * 从 Class 创建 OutputType。
     */
    public static OutputType of(Class<?> clazz) {
        return new OutputType(new ParameterizedTypeReference<>() {}, clazz);
    }

    /**
     * 从 ParameterizedTypeReference 创建 OutputType。
     */
    public static OutputType of(ParameterizedTypeReference<?> typeReference) {
        return new OutputType(typeReference, null);
    }

    public ParameterizedTypeReference<?> toTypeReference() {
        return typeReference;
    }

    public Class<?> getRawType() {
        return rawType;
    }
}
