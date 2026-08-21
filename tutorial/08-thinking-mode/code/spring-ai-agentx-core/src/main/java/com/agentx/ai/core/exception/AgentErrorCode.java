package com.agentx.ai.core.exception;

/**
 * Agent 错误码。
 */
public enum AgentErrorCode {

    /** LLM 调用失败（网络异常、服务端错误、重试耗尽） */
    LLM_CALL_FAILED,

    /** 并发执行冲突（同一会话重复调用） */
    CONCURRENT_EXECUTION,

    /** 工具执行失败 */
    TOOL_EXECUTION_FAILED,

    /** 参数非法 */
    INVALID_PARAMS
}
