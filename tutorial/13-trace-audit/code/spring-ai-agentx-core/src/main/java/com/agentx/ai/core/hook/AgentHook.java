package com.agentx.ai.core.hook;

/**
 * Agent 生命周期 Hook — 统一事件模型。
 * <p>
 * 实现者用模式匹配处理感兴趣的事件。
 *
 * @author bigchui
 */
public interface AgentHook {

    /**
     * 处理事件。返回传入的 event（可能已被修改）。返回 null 时使用原事件。
     */
    HookEvent onEvent(HookEvent event);

    /**
     * 优先级，数值越大越先执行。默认 0。
     */
    default int priority() {
        return 0;
    }
}
