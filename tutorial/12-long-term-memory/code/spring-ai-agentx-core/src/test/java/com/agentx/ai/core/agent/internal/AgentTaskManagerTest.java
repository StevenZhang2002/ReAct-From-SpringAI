package com.agentx.ai.core.agent.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentTaskManager 单元测试（第07节）。
 *
 * <p>测试任务注册、停止、清理、并发控制。
 */
@DisplayName("AgentTaskManager 测试")
class AgentTaskManagerTest {

    private AgentTaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = new AgentTaskManager();
    }

    @Nested
    @DisplayName("registerTask() 方法测试")
    class RegisterTaskTests {

        @Test
        @DisplayName("注册新任务")
        void shouldRegisterNewTask() {
            // Given
            String conversationId = "conv-123";
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

            // When
            AgentTaskManager.TaskInfo taskInfo = taskManager.registerTask(conversationId, sink);

            // Then
            assertNotNull(taskInfo);
            assertEquals(sink, taskInfo.getSink());
            assertTrue(taskManager.hasRunningTask(conversationId));
        }

        @Test
        @DisplayName("同一会话不能并发注册")
        void shouldNotRegisterSameConversationTwice() {
            // Given
            String conversationId = "conv-123";
            Sinks.Many<String> sink1 = Sinks.many().multicast().onBackpressureBuffer();
            Sinks.Many<String> sink2 = Sinks.many().multicast().onBackpressureBuffer();

            // When
            AgentTaskManager.TaskInfo task1 = taskManager.registerTask(conversationId, sink1);
            AgentTaskManager.TaskInfo task2 = taskManager.registerTask(conversationId, sink2);

            // Then
            assertNotNull(task1);
            assertNull(task2);
            assertEquals(1, taskManager.getTaskCount());
        }

        @Test
        @DisplayName("不同会话可以并发注册")
        void shouldRegisterDifferentConversations() {
            // Given
            Sinks.Many<String> sink1 = Sinks.many().multicast().onBackpressureBuffer();
            Sinks.Many<String> sink2 = Sinks.many().multicast().onBackpressureBuffer();

            // When
            AgentTaskManager.TaskInfo task1 = taskManager.registerTask("conv-1", sink1);
            AgentTaskManager.TaskInfo task2 = taskManager.registerTask("conv-2", sink2);

            // Then
            assertNotNull(task1);
            assertNotNull(task2);
            assertEquals(2, taskManager.getTaskCount());
        }

        @Test
        @DisplayName("conversationId 为 null 时直接返回 TaskInfo")
        void shouldReturnTaskInfoWhenConversationIdIsNull() {
            // Given
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

            // When
            AgentTaskManager.TaskInfo taskInfo = taskManager.registerTask(null, sink);

            // Then
            assertNotNull(taskInfo);
            assertEquals(0, taskManager.getTaskCount()); // null 不存入 map
        }
    }

    @Nested
    @DisplayName("stopTask() 方法测试")
    class StopTaskTests {

        @Test
        @DisplayName("停止存在的任务")
        void shouldStopExistingTask() {
            // Given
            String conversationId = "conv-123";
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
            taskManager.registerTask(conversationId, sink);

            // When
            boolean result = taskManager.stopTask(conversationId);

            // Then
            assertTrue(result);
            assertFalse(taskManager.hasRunningTask(conversationId));
        }

        @Test
        @DisplayName("停止不存在的任务返回 false")
        void shouldReturnFalseWhenTaskNotExists() {
            // When
            boolean result = taskManager.stopTask("non-existent");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("停止 null 会话返回 false")
        void shouldReturnFalseWhenConversationIdIsNull() {
            // When
            boolean result = taskManager.stopTask(null);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("removeTask() 方法测试")
    class RemoveTaskTests {

        @Test
        @DisplayName("移除任务")
        void shouldRemoveTask() {
            // Given
            String conversationId = "conv-123";
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
            taskManager.registerTask(conversationId, sink);
            assertTrue(taskManager.hasRunningTask(conversationId));

            // When
            taskManager.removeTask(conversationId);

            // Then
            assertFalse(taskManager.hasRunningTask(conversationId));
            assertEquals(0, taskManager.getTaskCount());
        }

        @Test
        @DisplayName("移除不存在的任务不抛异常")
        void shouldNotThrowWhenTaskNotExists() {
            // When & Then
            assertDoesNotThrow(() -> taskManager.removeTask("non-existent"));
        }

        @Test
        @DisplayName("移除 null 会话不抛异常")
        void shouldNotThrowWhenConversationIdIsNull() {
            // When & Then
            assertDoesNotThrow(() -> taskManager.removeTask(null));
        }
    }

    @Nested
    @DisplayName("hasRunningTask() 方法测试")
    class HasRunningTaskTests {

        @Test
        @DisplayName("检查存在的任务")
        void shouldReturnTrueForExistingTask() {
            // Given
            String conversationId = "conv-123";
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
            taskManager.registerTask(conversationId, sink);

            // Then
            assertTrue(taskManager.hasRunningTask(conversationId));
        }

        @Test
        @DisplayName("检查不存在的任务")
        void shouldReturnFalseForNonExistingTask() {
            // Then
            assertFalse(taskManager.hasRunningTask("non-existent"));
        }

        @Test
        @DisplayName("检查 null 会话返回 false")
        void shouldReturnFalseForNullConversationId() {
            // Then
            assertFalse(taskManager.hasRunningTask(null));
        }
    }

    @Nested
    @DisplayName("getTaskCount() 方法测试")
    class GetTaskCountTests {

        @Test
        @DisplayName("初始任务数为 0")
        void shouldReturnZeroInitially() {
            assertEquals(0, taskManager.getTaskCount());
        }

        @Test
        @DisplayName("注册后任务数增加")
        void shouldIncreaseAfterRegister() {
            // Given
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

            // When
            taskManager.registerTask("conv-1", sink);
            taskManager.registerTask("conv-2", sink);

            // Then
            assertEquals(2, taskManager.getTaskCount());
        }

        @Test
        @DisplayName("停止后任务数减少")
        void shouldDecreaseAfterStop() {
            // Given
            String conversationId = "conv-123";
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
            taskManager.registerTask(conversationId, sink);
            assertEquals(1, taskManager.getTaskCount());

            // When
            taskManager.stopTask(conversationId);

            // Then
            assertEquals(0, taskManager.getTaskCount());
        }
    }

    @Nested
    @DisplayName("TaskInfo 测试")
    class TaskInfoTests {

        @Test
        @DisplayName("TaskInfo 创建时间")
        void shouldHaveCreateTime() {
            // Given
            long before = System.currentTimeMillis();
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

            // When
            AgentTaskManager.TaskInfo taskInfo = new AgentTaskManager.TaskInfo(sink);
            long after = System.currentTimeMillis();

            // Then
            assertTrue(taskInfo.getCreateTime() >= before);
            assertTrue(taskInfo.getCreateTime() <= after);
        }

        @Test
        @DisplayName("TaskInfo 设置 Disposable")
        void shouldSetDisposable() {
            // Given
            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
            AgentTaskManager.TaskInfo taskInfo = new AgentTaskManager.TaskInfo(sink);
            assertNull(taskInfo.getDisposable());

            // When
            taskInfo.setDisposable(() -> {});

            // Then
            assertNotNull(taskInfo.getDisposable());
        }
    }
}
