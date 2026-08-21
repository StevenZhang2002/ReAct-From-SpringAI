package com.agentx.ai.core.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ContextPolicy 单元测试（main 分支完整版）。
 */
@DisplayName("ContextPolicy 测试")
class ContextPolicyTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultTests {

        @Test
        @DisplayName("默认 lastKeep 值")
        void shouldHaveDefaultLastKeep() {
            assertEquals(50, ContextPolicy.DEFAULT_LAST_KEEP);
        }

        @Test
        @DisplayName("默认 msgThreshold 值")
        void shouldHaveDefaultMsgThreshold() {
            assertEquals(100, ContextPolicy.DEFAULT_MSG_THRESHOLD);
        }

        @Test
        @DisplayName("默认 tokenThreshold 值")
        void shouldHaveDefaultTokenThreshold() {
            assertEquals(90000, ContextPolicy.DEFAULT_TOKEN_THRESHOLD);
        }

        @Test
        @DisplayName("defaults() 返回默认配置")
        void shouldReturnDefaultPolicy() {
            ContextPolicy policy = ContextPolicy.defaults();
            assertEquals(50, policy.lastKeep());
            assertEquals(100, policy.msgThreshold());
            assertEquals(90000, policy.tokenThreshold());
        }
    }

    @Nested
    @DisplayName("Builder 模式测试")
    class BuilderTests {

        @Test
        @DisplayName("自定义配置")
        void shouldBuildCustomPolicy() {
            ContextPolicy policy = ContextPolicy.builder()
                    .lastKeep(10)
                    .msgThreshold(50)
                    .tokenThreshold(50000)
                    .build();
            assertEquals(10, policy.lastKeep());
            assertEquals(50, policy.msgThreshold());
            assertEquals(50000, policy.tokenThreshold());
        }

        @Test
        @DisplayName("使用默认值")
        void shouldUseDefaultValues() {
            ContextPolicy policy = ContextPolicy.builder().build();
            assertEquals(ContextPolicy.DEFAULT_LAST_KEEP, policy.lastKeep());
            assertEquals(ContextPolicy.DEFAULT_MSG_THRESHOLD, policy.msgThreshold());
            assertEquals(ContextPolicy.DEFAULT_TOKEN_THRESHOLD, policy.tokenThreshold());
        }

        @Test
        @DisplayName("部分覆盖")
        void shouldPartiallyOverride() {
            ContextPolicy policy = ContextPolicy.builder().lastKeep(20).build();
            assertEquals(20, policy.lastKeep());
            assertEquals(ContextPolicy.DEFAULT_MSG_THRESHOLD, policy.msgThreshold());
        }
    }

    @Nested
    @DisplayName("Record 特性测试")
    class RecordTests {

        @Test
        @DisplayName("record 相等性")
        void shouldBeEqualForSameValues() {
            ContextPolicy p1 = new ContextPolicy(10, 50, 50000);
            ContextPolicy p2 = new ContextPolicy(10, 50, 50000);
            assertEquals(p1, p2);
            assertEquals(p1.hashCode(), p2.hashCode());
        }

        @Test
        @DisplayName("record 不相等")
        void shouldNotBeEqualForDifferentValues() {
            ContextPolicy p1 = new ContextPolicy(10, 50, 50000);
            ContextPolicy p2 = new ContextPolicy(20, 50, 50000);
            assertNotEquals(p1, p2);
        }
    }
}
