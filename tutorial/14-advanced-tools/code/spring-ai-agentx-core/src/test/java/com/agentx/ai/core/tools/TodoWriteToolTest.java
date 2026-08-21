package com.agentx.ai.core.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TodoWriteTool 单元测试（第14节）。
 *
 * <p>测试任务验证、状态校验。
 */
@DisplayName("TodoWriteTool 测试")
class TodoWriteToolTest {

    private TodoWriteTool tool;

    @BeforeEach
    void setUp() {
        tool = new TodoWriteTool();
    }

    @Nested
    @DisplayName("todoWrite() 方法测试")
    class TodoWriteTests {

        @Test
        @DisplayName("有效任务列表")
        void shouldAcceptValidTodos() {
            // Given
            List<TodoWriteTool.TodoItem> todos = List.of(
                    new TodoWriteTool.TodoItem("任务1", TodoWriteTool.Status.pending, "正在执行任务1"),
                    new TodoWriteTool.TodoItem("任务2", TodoWriteTool.Status.pending, "正在执行任务2")
            );

            // When
            String result = tool.todoWrite(todos);

            // Then
            assertNotNull(result);
            assertTrue(result.contains("成功"));
        }

        @Test
        @DisplayName("单个 in_progress 任务")
        void shouldAcceptSingleInProgress() {
            // Given
            List<TodoWriteTool.TodoItem> todos = List.of(
                    new TodoWriteTool.TodoItem("任务1", TodoWriteTool.Status.in_progress, "正在执行任务1")
            );

            // When
            String result = tool.todoWrite(todos);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("空列表抛出异常")
        void shouldThrowOnEmptyList() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                    tool.todoWrite(List.of()));
        }

        @Test
        @DisplayName("null 列表抛出异常")
        void shouldThrowOnNullList() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                    tool.todoWrite(null));
        }

        @Test
        @DisplayName("多个 in_progress 任务抛出异常")
        void shouldThrowOnMultipleInProgress() {
            // Given
            List<TodoWriteTool.TodoItem> todos = List.of(
                    new TodoWriteTool.TodoItem("任务1", TodoWriteTool.Status.in_progress, "正在执行任务1"),
                    new TodoWriteTool.TodoItem("任务2", TodoWriteTool.Status.in_progress, "正在执行任务2")
            );

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    tool.todoWrite(todos));
            assertTrue(exception.getMessage().contains("in_progress"));
        }

        @Test
        @DisplayName("任务 content 为空抛出异常")
        void shouldThrowOnEmptyContent() {
            // Given
            List<TodoWriteTool.TodoItem> todos = List.of(
                    new TodoWriteTool.TodoItem("", TodoWriteTool.Status.pending, "正在执行")
            );

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                    tool.todoWrite(todos));
        }

        @Test
        @DisplayName("任务 content 为空白抛出异常")
        void shouldThrowOnBlankContent() {
            // Given
            List<TodoWriteTool.TodoItem> todos = List.of(
                    new TodoWriteTool.TodoItem("   ", TodoWriteTool.Status.pending, "正在执行")
            );

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                    tool.todoWrite(todos));
        }

        @Test
        @DisplayName("任务 activeForm 为空抛出异常")
        void shouldThrowOnEmptyActiveForm() {
            // Given
            List<TodoWriteTool.TodoItem> todos = List.of(
                    new TodoWriteTool.TodoItem("任务内容", TodoWriteTool.Status.pending, "")
            );

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                    tool.todoWrite(todos));
        }

        @Test
        @DisplayName("任务 status 为 null 抛出异常")
        void shouldThrowOnNullStatus() {
            // Given
            List<TodoWriteTool.TodoItem> todos = List.of(
                    new TodoWriteTool.TodoItem("任务内容", null, "正在执行")
            );

            // When & Then
            assertThrows(NullPointerException.class, () ->
                    tool.todoWrite(todos));
        }

        @Test
        @DisplayName("任务为 null 抛出异常")
        void shouldThrowOnNullItem() {
            // Given
            List<TodoWriteTool.TodoItem> todos = new java.util.ArrayList<>();
            todos.add(null);

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                    tool.todoWrite(todos));
        }
    }

    @Nested
    @DisplayName("TodoItem record 测试")
    class TodoItemTests {

        @Test
        @DisplayName("创建 TodoItem")
        void shouldCreateTodoItem() {
            // When
            TodoWriteTool.TodoItem item = new TodoWriteTool.TodoItem(
                    "任务内容",
                    TodoWriteTool.Status.pending,
                    "正在执行"
            );

            // Then
            assertEquals("任务内容", item.content());
            assertEquals(TodoWriteTool.Status.pending, item.status());
            assertEquals("正在执行", item.activeForm());
        }

        @Test
        @DisplayName("TodoItem 相等性")
        void shouldBeEqualForSameValues() {
            // Given
            TodoWriteTool.TodoItem item1 = new TodoWriteTool.TodoItem(
                    "任务", TodoWriteTool.Status.pending, "执行中");
            TodoWriteTool.TodoItem item2 = new TodoWriteTool.TodoItem(
                    "任务", TodoWriteTool.Status.pending, "执行中");

            // Then
            assertEquals(item1, item2);
            assertEquals(item1.hashCode(), item2.hashCode());
        }
    }

    @Nested
    @DisplayName("Status 枚举测试")
    class StatusTests {

        @Test
        @DisplayName("Status 有三个值")
        void shouldHaveThreeValues() {
            assertEquals(3, TodoWriteTool.Status.values().length);
        }

        @Test
        @DisplayName("pending 状态")
        void shouldHavePending() {
            assertNotNull(TodoWriteTool.Status.pending);
        }

        @Test
        @DisplayName("in_progress 状态")
        void shouldHaveInProgress() {
            assertNotNull(TodoWriteTool.Status.in_progress);
        }

        @Test
        @DisplayName("completed 状态")
        void shouldHaveCompleted() {
            assertNotNull(TodoWriteTool.Status.completed);
        }

        @Test
        @DisplayName("valueOf 测试")
        void shouldSupportValueOf() {
            assertEquals(TodoWriteTool.Status.pending, TodoWriteTool.Status.valueOf("pending"));
            assertEquals(TodoWriteTool.Status.in_progress, TodoWriteTool.Status.valueOf("in_progress"));
            assertEquals(TodoWriteTool.Status.completed, TodoWriteTool.Status.valueOf("completed"));
        }
    }

    @Nested
    @DisplayName("create() 工厂方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建 ToolCallback 数组")
        void shouldCreateToolCallbacks() {
            // When
            var callbacks = TodoWriteTool.create();

            // Then
            assertNotNull(callbacks);
            assertTrue(callbacks.length > 0);
        }
    }

    @Nested
    @DisplayName("混合状态测试")
    class MixedStatusTests {

        @Test
        @DisplayName("pending + in_progress + completed 混合")
        void shouldAcceptMixedStatus() {
            // Given
            List<TodoWriteTool.TodoItem> todos = List.of(
                    new TodoWriteTool.TodoItem("已完成任务", TodoWriteTool.Status.completed, "已完成"),
                    new TodoWriteTool.TodoItem("进行中任务", TodoWriteTool.Status.in_progress, "正在执行"),
                    new TodoWriteTool.TodoItem("待处理任务", TodoWriteTool.Status.pending, "等待中")
            );

            // When
            String result = tool.todoWrite(todos);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("全部 pending")
        void shouldAcceptAllPending() {
            // Given
            List<TodoWriteTool.TodoItem> todos = List.of(
                    new TodoWriteTool.TodoItem("任务1", TodoWriteTool.Status.pending, "等待1"),
                    new TodoWriteTool.TodoItem("任务2", TodoWriteTool.Status.pending, "等待2"),
                    new TodoWriteTool.TodoItem("任务3", TodoWriteTool.Status.pending, "等待3")
            );

            // When
            String result = tool.todoWrite(todos);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("全部 completed")
        void shouldAcceptAllCompleted() {
            // Given
            List<TodoWriteTool.TodoItem> todos = List.of(
                    new TodoWriteTool.TodoItem("任务1", TodoWriteTool.Status.completed, "完成1"),
                    new TodoWriteTool.TodoItem("任务2", TodoWriteTool.Status.completed, "完成2")
            );

            // When
            String result = tool.todoWrite(todos);

            // Then
            assertNotNull(result);
        }
    }
}
