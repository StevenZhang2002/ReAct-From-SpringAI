package com.agentx.ai.core.interrupt;

/**
 * 暂停原因。
 */
public enum PauseReason {
    /** HITL 工具审批（ask_user 工具被拦截） */
    HITL_TOOL_REQUEST,
    /** 用户主动中断 */
    USER_INTERRUPT
}
