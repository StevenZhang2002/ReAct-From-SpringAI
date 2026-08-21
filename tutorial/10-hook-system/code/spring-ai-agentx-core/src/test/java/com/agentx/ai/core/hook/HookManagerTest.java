package com.agentx.ai.core.hook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HookManager 单元测试（第10节）。
 *
 * <p>测试 Hook 注册、优先级排序、事件触发、异常处理。
 */
@DisplayName("HookManager 测试")
class HookManagerTest {

    @Nested
    @DisplayName("构造测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用 null 创建 HookManager")
        void shouldCreateWithNull() {
            // When
            HookManager manager = new HookManager(null);

            // Then
            assertTrue(manager.isEmpty());
        }

        @Test
        @DisplayName("使用空列表创建 HookManager")
        void shouldCreateWithEmptyList() {
            // When
            HookManager manager = new HookManager(List.of());

            // Then
            assertTrue(manager.isEmpty());
        }

        @Test
        @DisplayName("使用 Hook 列表创建 HookManager")
        void shouldCreateWithHooks() {
            // Given
            AgentHook hook = createTestHook(0);

            // When
            HookManager manager = new HookManager(List.of(hook));

            // Then
            assertFalse(manager.isEmpty());
        }
    }

    @Nested
    @DisplayName("EMPTY 常量测试")
    class EmptyTests {

        @Test
        @DisplayName("EMPTY 是空的")
        void shouldBeEmpty() {
            assertTrue(HookManager.EMPTY.isEmpty());
        }
    }

    @Nested
    @DisplayName("fireEvent() 方法测试")
    class FireEventTests {

        @Test
        @DisplayName("空 HookManager 直接返回事件")
        void shouldReturnEventWhenEmpty() {
            // Given
            HookManager manager = HookManager.EMPTY;
            BeforeCallEvent event = new BeforeCallEvent("测试", null);

            // When
            BeforeCallEvent result = manager.fireEvent(event);

            // Then
            assertEquals(event, result);
        }

        @Test
        @DisplayName("触发单个 Hook")
        void shouldFireSingleHook() {
            // Given
            AtomicInteger callCount = new AtomicInteger(0);
            AgentHook hook = new AgentHook() {
                @Override
                public HookEvent onEvent(HookEvent event) {
                    callCount.incrementAndGet();
                    return event;
                }

                @Override
                public int priority() {
                    return 0;
                }
            };

            HookManager manager = new HookManager(List.of(hook));
            BeforeCallEvent event = new BeforeCallEvent("测试", null);

            // When
            manager.fireEvent(event);

            // Then
            assertEquals(1, callCount.get());
        }

        @Test
        @DisplayName("触发多个 Hook")
        void shouldFireMultipleHooks() {
            // Given
            AtomicInteger callCount = new AtomicInteger(0);
            AgentHook hook1 = createCountingHook(callCount, 0);
            AgentHook hook2 = createCountingHook(callCount, 0);

            HookManager manager = new HookManager(List.of(hook1, hook2));
            BeforeCallEvent event = new BeforeCallEvent("测试", null);

            // When
            manager.fireEvent(event);

            // Then
            assertEquals(2, callCount.get());
        }

        @Test
        @DisplayName("按优先级排序触发")
        void shouldFireInPriorityOrder() {
            // Given
            StringBuilder order = new StringBuilder();
            AgentHook hook1 = createOrderHook(order, "A", 1);
            AgentHook hook2 = createOrderHook(order, "B", 10);
            AgentHook hook3 = createOrderHook(order, "C", 5);

            HookManager manager = new HookManager(List.of(hook1, hook2, hook3));
            BeforeCallEvent event = new BeforeCallEvent("测试", null);

            // When
            manager.fireEvent(event);

            // Then - 优先级高的先执行
            assertEquals("B", order.toString().substring(0, 1));
        }

        @Test
        @DisplayName("Hook 异常不影响其他 Hook")
        void shouldContinueWhenHookThrowsException() {
            // Given
            AtomicInteger callCount = new AtomicInteger(0);
            AgentHook throwingHook = new AgentHook() {
                @Override
                public HookEvent onEvent(HookEvent event) {
                    callCount.incrementAndGet();
                    throw new RuntimeException("测试异常");
                }

                @Override
                public int priority() {
                    return 10;
                }
            };
            AgentHook normalHook = createCountingHook(callCount, 0);

            HookManager manager = new HookManager(List.of(throwingHook, normalHook));
            BeforeCallEvent event = new BeforeCallEvent("测试", null);

            // When
            assertDoesNotThrow(() -> manager.fireEvent(event));

            // Then - 两个 Hook 都被调用
            assertEquals(2, callCount.get());
        }

        @Test
        @DisplayName("Hook 可以修改事件")
        void shouldAllowHookToModifyEvent() {
            // Given
            AgentHook modifyingHook = new AgentHook() {
                @Override
                public HookEvent onEvent(HookEvent event) {
                    if (event instanceof BeforeCallEvent e) {
                        return new BeforeCallEvent("修改后的查询", e.params());
                    }
                    return event;
                }

                @Override
                public int priority() {
                    return 0;
                }
            };

            HookManager manager = new HookManager(List.of(modifyingHook));
            BeforeCallEvent event = new BeforeCallEvent("原始查询", null);

            // When
            BeforeCallEvent result = manager.fireEvent(event);

            // Then
            assertEquals("修改后的查询", result.query());
        }
    }

    @Nested
    @DisplayName("优先级测试")
    class PriorityTests {

        @Test
        @DisplayName("相同优先级的 Hook 按注册顺序执行")
        void shouldExecuteSamePriorityInOrder() {
            // Given
            StringBuilder order = new StringBuilder();
            AgentHook hook1 = createOrderHook(order, "A", 5);
            AgentHook hook2 = createOrderHook(order, "B", 5);

            HookManager manager = new HookManager(List.of(hook1, hook2));
            BeforeCallEvent event = new BeforeCallEvent("测试", null);

            // When
            manager.fireEvent(event);

            // Then
            assertEquals("AB", order.toString());
        }

        @Test
        @DisplayName("不同优先级的 Hook 按优先级降序执行")
        void shouldExecuteInDescendingPriorityOrder() {
            // Given
            StringBuilder order = new StringBuilder();
            AgentHook hook1 = createOrderHook(order, "Low", 1);
            AgentHook hook2 = createOrderHook(order, "High", 100);
            AgentHook hook3 = createOrderHook(order, "Medium", 50);

            HookManager manager = new HookManager(List.of(hook1, hook2, hook3));
            BeforeCallEvent event = new BeforeCallEvent("测试", null);

            // When
            manager.fireEvent(event);

            // Then
            assertEquals("HighMediumLow", order.toString());
        }
    }

    // ==================== 辅助方法 ====================

    private AgentHook createTestHook(int priority) {
        return new AgentHook() {
            @Override
            public HookEvent onEvent(HookEvent event) {
                return event;
            }

            @Override
            public int priority() {
                return priority;
            }
        };
    }

    private AgentHook createCountingHook(AtomicInteger counter, int priority) {
        return new AgentHook() {
            @Override
            public HookEvent onEvent(HookEvent event) {
                counter.incrementAndGet();
                return event;
            }

            @Override
            public int priority() {
                return priority;
            }
        };
    }

    private AgentHook createOrderHook(StringBuilder order, String name, int priority) {
        return new AgentHook() {
            @Override
            public HookEvent onEvent(HookEvent event) {
                order.append(name);
                return event;
            }

            @Override
            public int priority() {
                return priority;
            }
        };
    }
}
