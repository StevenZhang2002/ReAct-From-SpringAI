package com.agentx.ai.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OutputType 单元测试。
 */
@DisplayName("OutputType 测试")
class OutputTypeTest {

    @Nested
    @DisplayName("of() 测试")
    class OfTests {

        @Test
        @DisplayName("创建单类型 OutputType")
        void shouldCreateSingleType() {
            OutputType type = OutputType.of(String.class);
            assertNotNull(type);
            assertEquals(String.class, type.getType());
        }

        @Test
        @DisplayName("创建自定义类 OutputType")
        void shouldCreateCustomType() {
            OutputType type = OutputType.of(TestPojo.class);
            assertEquals(TestPojo.class, type.getType());
        }
    }

    @Nested
    @DisplayName("listOf() 测试")
    class ListOfTests {

        @Test
        @DisplayName("创建 List 类型 OutputType")
        void shouldCreateListType() {
            OutputType type = OutputType.listOf(String.class);
            assertNotNull(type);
            assertNotNull(type.getType());
        }

        @Test
        @DisplayName("toTypeReference 不为 null")
        void shouldCreateTypeReference() {
            OutputType type = OutputType.listOf(String.class);
            assertNotNull(type.toTypeReference());
        }
    }

    // 测试用 POJO
    private static class TestPojo {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
