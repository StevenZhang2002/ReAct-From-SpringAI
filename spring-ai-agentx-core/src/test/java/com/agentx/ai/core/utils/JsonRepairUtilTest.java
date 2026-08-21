package com.agentx.ai.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonRepairUtil 单元测试。
 *
 * <p>测试 JSON 修复、Markdown 提取、引号修复、尾部逗号修复等。
 */
@DisplayName("JsonRepairUtil 测试")
class JsonRepairUtilTest {

    @Nested
    @DisplayName("fixJson() 基础测试")
    class FixJsonBasicTests {

        @Test
        @DisplayName("有效 JSON 保持不变")
        void shouldKeepValidJson() {
            String json = "{\"name\":\"Alice\",\"age\":30}";
            String result = JsonRepairUtil.fixJson(json);
            assertTrue(JsonRepairUtil.isValidJson(result));
        }

        @Test
        @DisplayName("null 返回空对象")
        void shouldReturnEmptyForNull() {
            assertEquals("{}", JsonRepairUtil.fixJson(null));
        }

        @Test
        @DisplayName("空字符串返回空对象")
        void shouldReturnEmptyForBlank() {
            assertEquals("{}", JsonRepairUtil.fixJson(""));
            assertEquals("{}", JsonRepairUtil.fixJson("   "));
        }

        @Test
        @DisplayName("有效数组保持不变")
        void shouldKeepValidArray() {
            String json = "[1,2,3]";
            String result = JsonRepairUtil.fixJson(json);
            assertTrue(JsonRepairUtil.isValidJson(result));
        }
    }

    @Nested
    @DisplayName("Markdown 代码块提取")
    class MarkdownExtractionTests {

        @Test
        @DisplayName("提取 ```json ... ``` 中的 JSON")
        void shouldExtractFromJsonCodeBlock() {
            String input = "```json\n{\"key\":\"value\"}\n```";
            String result = JsonRepairUtil.fixJson(input);
            assertTrue(JsonRepairUtil.isValidJson(result));
            assertTrue(result.contains("key"));
        }

        @Test
        @DisplayName("提取 ``` ... ``` 中的 JSON")
        void shouldExtractFromPlainCodeBlock() {
            String input = "```\n{\"key\":\"value\"}\n```";
            String result = JsonRepairUtil.fixJson(input);
            assertTrue(JsonRepairUtil.isValidJson(result));
        }

        @Test
        @DisplayName("前后有垃圾字符的 JSON")
        void shouldExtractJsonWithGarbage() {
            String input = "some text before {\"key\":\"value\"} some text after";
            String result = JsonRepairUtil.fixJson(input);
            assertTrue(JsonRepairUtil.isValidJson(result));
        }
    }

    @Nested
    @DisplayName("引号修复")
    class QuoteFixTests {

        @Test
        @DisplayName("中文双引号替换为英文双引号")
        void shouldFixChineseDoubleQuotes() {
            String input = "{\"name\":\u201cAlice\u201d}";
            String result = JsonRepairUtil.fixJson(input);
            assertTrue(JsonRepairUtil.isValidJson(result));
            assertTrue(result.contains("\"Alice\""));
        }

        @Test
        @DisplayName("单引号替换为双引号（结构位置）")
        void shouldFixSingleQuotes() {
            String input = "{'name':'Alice'}";
            String result = JsonRepairUtil.fixJson(input);
            assertTrue(JsonRepairUtil.isValidJson(result));
        }
    }

    @Nested
    @DisplayName("尾部逗号修复")
    class TrailingCommaTests {

        @Test
        @DisplayName("移除对象尾部逗号")
        void shouldRemoveTrailingCommaInObject() {
            String input = "{\"name\":\"Alice\",}";
            String result = JsonRepairUtil.fixJson(input);
            assertTrue(JsonRepairUtil.isValidJson(result));
            assertFalse(result.contains(",}"));
        }

        @Test
        @DisplayName("移除数组尾部逗号")
        void shouldRemoveTrailingCommaInArray() {
            String input = "[1,2,3,]";
            String result = JsonRepairUtil.fixJson(input);
            assertTrue(JsonRepairUtil.isValidJson(result));
            assertFalse(result.contains(",]"));
        }
    }

    @Nested
    @DisplayName("缺失引号修复")
    class MissingQuotesTests {

        @Test
        @DisplayName("为无引号的键添加引号")
        void shouldAddQuotesToUnquotedKeys() {
            String input = "{name:\"Alice\"}";
            String result = JsonRepairUtil.fixJson(input);
            assertTrue(JsonRepairUtil.isValidJson(result));
            assertTrue(result.contains("\"name\""));
        }
    }

    @Nested
    @DisplayName("fixAndParse() 测试")
    class FixAndParseTests {

        @Test
        @DisplayName("解析有效 JSON")
        void shouldParseValidJson() {
            JsonNode node = JsonRepairUtil.fixAndParse("{\"key\":\"value\"}");
            assertEquals("value", node.get("key").asText());
        }

        @Test
        @DisplayName("解析无效 JSON 返回空对象")
        void shouldReturnEmptyNodeForInvalidJson() {
            JsonNode node = JsonRepairUtil.fixAndParse("not json at all !!!");
            assertNotNull(node);
            assertTrue(node.isObject());
        }

        @Test
        @DisplayName("解析 null 返回空对象")
        void shouldReturnEmptyNodeForNull() {
            JsonNode node = JsonRepairUtil.fixAndParse(null);
            assertNotNull(node);
            assertTrue(node.isObject());
        }
    }

    @Nested
    @DisplayName("isValidJson() 测试")
    class IsValidJsonTests {

        @Test
        @DisplayName("有效对象")
        void shouldBeValidForObject() {
            assertTrue(JsonRepairUtil.isValidJson("{\"a\":1}"));
        }

        @Test
        @DisplayName("有效数组")
        void shouldBeValidForArray() {
            assertTrue(JsonRepairUtil.isValidJson("[1,2]"));
        }

        @Test
        @DisplayName("无效 JSON")
        void shouldBeInvalid() {
            assertFalse(JsonRepairUtil.isValidJson("not json"));
        }

        @Test
        @DisplayName("null 无效")
        void shouldBeInvalidForNull() {
            assertFalse(JsonRepairUtil.isValidJson(null));
        }
    }

    @Nested
    @DisplayName("prettify() 测试")
    class PrettifyTests {

        @Test
        @DisplayName("格式化 JSON")
        void shouldPrettifyJson() {
            String input = "{\"name\":\"Alice\"}";
            String result = JsonRepairUtil.prettify(input);
            assertTrue(result.contains("\n"));
            assertTrue(result.contains("Alice"));
        }

        @Test
        @DisplayName("无效 JSON 返回原文")
        void shouldReturnOriginalForInvalid() {
            String input = "not json";
            assertEquals(input, JsonRepairUtil.prettify(input));
        }
    }
}
