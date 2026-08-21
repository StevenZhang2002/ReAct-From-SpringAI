package com.agentx.ai.core.interrupt;

import com.agentx.ai.core.model.PauseState;
import com.agentx.ai.core.model.RunnableParams;
import com.agentx.ai.core.model.PendingToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InMemoryPauseStateStore 单元测试。
 */
@DisplayName("InMemoryPauseStateStore 测试")
class InMemoryPauseStateStoreTest {

    private InMemoryPauseStateStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryPauseStateStore();
    }

    @Nested
    @DisplayName("save() 测试")
    class SaveTests {

        @Test
        @DisplayName("保存 PauseState")
        void shouldSaveState() {
            PauseState state = buildState("conv-1");
            store.save(state);
            assertTrue(store.exists("conv-1"));
        }

        @Test
        @DisplayName("state 为 null 抛异常")
        void shouldThrowOnNullState() {
            assertThrows(NullPointerException.class, () -> store.save(null));
        }

        @Test
        @DisplayName("conversationId 为 null 抛异常")
        void shouldThrowOnNullConversationId() {
            PauseState state = PauseState.builder().build();
            assertThrows(IllegalArgumentException.class, () -> store.save(state));
        }

        @Test
        @DisplayName("覆盖已有记录")
        void shouldOverwriteExisting() {
            PauseState state1 = PauseState.builder()
                    .params(RunnableParams.builder().conversationId("conv-1").build())
                    .currentRound(1)
                    .build();
            PauseState state2 = PauseState.builder()
                    .params(RunnableParams.builder().conversationId("conv-1").build())
                    .currentRound(5)
                    .build();

            store.save(state1);
            store.save(state2);

            PauseState found = store.findByConversationId("conv-1");
            assertEquals(5, found.getCurrentRound());
        }
    }

    @Nested
    @DisplayName("findByConversationId() 测试")
    class FindTests {

        @Test
        @DisplayName("查找存在的记录")
        void shouldFindExisting() {
            store.save(buildState("conv-1"));
            PauseState found = store.findByConversationId("conv-1");
            assertNotNull(found);
            assertEquals("conv-1", found.getParams().getConversationId());
        }

        @Test
        @DisplayName("查找不存在的记录返回 null")
        void shouldReturnNullForNonExisting() {
            assertNull(store.findByConversationId("non-existing"));
        }

        @Test
        @DisplayName("查找 null 返回 null")
        void shouldReturnNullForNull() {
            assertNull(store.findByConversationId(null));
        }
    }

    @Nested
    @DisplayName("exists() 测试")
    class ExistsTests {

        @Test
        @DisplayName("存在的记录返回 true")
        void shouldReturnTrueForExisting() {
            store.save(buildState("conv-1"));
            assertTrue(store.exists("conv-1"));
        }

        @Test
        @DisplayName("不存在的记录返回 false")
        void shouldReturnFalseForNonExisting() {
            assertFalse(store.exists("non-existing"));
        }

        @Test
        @DisplayName("null 返回 false")
        void shouldReturnFalseForNull() {
            assertFalse(store.exists(null));
        }
    }

    @Nested
    @DisplayName("delete() 测试")
    class DeleteTests {

        @Test
        @DisplayName("删除存在的记录返回 true")
        void shouldDeleteExisting() {
            store.save(buildState("conv-1"));
            assertTrue(store.delete("conv-1"));
            assertFalse(store.exists("conv-1"));
        }

        @Test
        @DisplayName("删除不存在的记录返回 false")
        void shouldReturnFalseForNonExisting() {
            assertFalse(store.delete("non-existing"));
        }

        @Test
        @DisplayName("删除 null 返回 false")
        void shouldReturnFalseForNull() {
            assertFalse(store.delete(null));
        }
    }

    @Nested
    @DisplayName("deleteExpired() 测试")
    class DeleteExpiredTests {

        @Test
        @DisplayName("未过期的记录不被删除")
        void shouldNotDeleteNonExpired() {
            PauseState state = PauseState.builder()
                    .params(RunnableParams.builder().conversationId("conv-1").build())
                    .interruptedAt(System.currentTimeMillis())
                    .build();
            store.save(state);

            int removed = store.deleteExpired();
            assertEquals(0, removed);
            assertTrue(store.exists("conv-1"));
        }

        @Test
        @DisplayName("已过期的记录被删除")
        void shouldDeleteExpired() {
            PauseState state = PauseState.builder()
                    .params(RunnableParams.builder().conversationId("conv-1").build())
                    .interruptedAt(System.currentTimeMillis() - 1000) // 1秒前
                    .build();

            // 使用很短的 TTL
            InMemoryPauseStateStore shortTtlStore = new InMemoryPauseStateStore(100); // 100ms
            shortTtlStore.save(state);

            int removed = shortTtlStore.deleteExpired();
            assertEquals(1, removed);
            assertFalse(shortTtlStore.exists("conv-1"));
        }

        @Test
        @DisplayName("interruptedAt 为 0 的记录永不过期")
        void shouldNeverExpireWhenInterruptedAtIsZero() {
            PauseState state = PauseState.builder()
                    .params(RunnableParams.builder().conversationId("conv-1").build())
                    .interruptedAt(0) // 永不过期
                    .build();
            store.save(state);

            int removed = store.deleteExpired();
            assertEquals(0, removed);
            assertTrue(store.exists("conv-1"));
        }
    }

    @Nested
    @DisplayName("构造测试")
    class ConstructorTests {

        @Test
        @DisplayName("自定义 TTL 构造")
        void shouldCreateWithCustomTtl() {
            InMemoryPauseStateStore customStore = new InMemoryPauseStateStore(60000);
            assertNotNull(customStore);
        }

        @Test
        @DisplayName("TTL <= 0 抛异常")
        void shouldThrowOnInvalidTtl() {
            assertThrows(IllegalArgumentException.class, () ->
                    new InMemoryPauseStateStore(0));
            assertThrows(IllegalArgumentException.class, () ->
                    new InMemoryPauseStateStore(-1));
        }
    }

    private PauseState buildState(String conversationId) {
        return PauseState.builder()
                .params(RunnableParams.builder().conversationId(conversationId).build())
                .currentRound(1)
                .reason(PauseReason.HITL_TOOL_REQUEST)
                .build();
    }
}
