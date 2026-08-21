package com.agentx.ai.core.exception;

/**
 * Agent 框架统一异常。
 */
public class AgentException extends RuntimeException {

    private final AgentErrorCode code;

    public AgentException(AgentErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public AgentException(AgentErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public AgentErrorCode getCode() {
        return code;
    }
}
