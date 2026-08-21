package com.agentx.ai.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MarkdownParser 单元测试。
 *
 * <p>测试 Front Matter 解析、内容提取。
 */
@DisplayName("MarkdownParser 测试")
class MarkdownParserTest {

    @Nested
    @DisplayName("Front Matter 解析")
    class FrontMatterTests {

        @Test
        @DisplayName("解析带 Front Matter 的文档")
        void shouldParseFrontMatter() {
            String md = """
                    ---
                    title: 测试文档
                    author: bigchui
                    ---
                    # 正文内容
                    """;
            MarkdownParser parser = new MarkdownParser(md);
            Map<String, Object> fm = parser.getFrontMatter();

            assertEquals("测试文档", fm.get("title"));
            assertEquals("bigchui", fm.get("author"));
        }

        @Test
        @DisplayName("解析带引号的值")
        void shouldParseQuotedValues() {
            String md = """
                    ---
                    title: "带引号的标题"
                    ---
                    内容
                    """;
            MarkdownParser parser = new MarkdownParser(md);
            assertEquals("带引号的标题", parser.getFrontMatter().get("title"));
        }

        @Test
        @DisplayName("解析单引号的值")
        void shouldParseSingleQuotedValues() {
            String md = """
                    ---
                    title: '单引号标题'
                    ---
                    内容
                    """;
            MarkdownParser parser = new MarkdownParser(md);
            assertEquals("单引号标题", parser.getFrontMatter().get("title"));
        }

        @Test
        @DisplayName("无 Front Matter 时返回空 Map")
        void shouldReturnEmptyMapWhenNoFrontMatter() {
            String md = "# 普通文档\n内容";
            MarkdownParser parser = new MarkdownParser(md);
            assertTrue(parser.getFrontMatter().isEmpty());
        }
    }

    @Nested
    @DisplayName("内容提取")
    class ContentTests {

        @Test
        @DisplayName("提取 Front Matter 后的内容")
        void shouldExtractContentAfterFrontMatter() {
            String md = """
                    ---
                    title: 标题
                    ---
                    # 正文
                    这是内容
                    """;
            MarkdownParser parser = new MarkdownParser(md);
            assertTrue(parser.getContent().contains("# 正文"));
            assertTrue(parser.getContent().contains("这是内容"));
        }

        @Test
        @DisplayName("无 Front Matter 时返回全部内容")
        void shouldReturnAllContentWhenNoFrontMatter() {
            String md = "# 标题\n这是内容";
            MarkdownParser parser = new MarkdownParser(md);
            assertEquals(md, parser.getContent());
        }
    }

    @Nested
    @DisplayName("边界情况")
    class EdgeCaseTests {

        @Test
        @DisplayName("null 输入")
        void shouldHandleNull() {
            MarkdownParser parser = new MarkdownParser(null);
            assertTrue(parser.getFrontMatter().isEmpty());
            assertEquals("", parser.getContent());
        }

        @Test
        @DisplayName("空字符串输入")
        void shouldHandleEmpty() {
            MarkdownParser parser = new MarkdownParser("");
            assertTrue(parser.getFrontMatter().isEmpty());
            assertEquals("", parser.getContent());
        }

        @Test
        @DisplayName("只有 --- 没有闭合")
        void shouldHandleUnclosedFrontMatter() {
            String md = "---\ntitle: 标题\n内容";
            MarkdownParser parser = new MarkdownParser(md);
            assertTrue(parser.getFrontMatter().isEmpty());
        }

        @Test
        @DisplayName("Front Matter 返回副本（不可修改原数据）")
        void shouldReturnCopyOfFrontMatter() {
            String md = "---\ntitle: 标题\n---\n内容";
            MarkdownParser parser = new MarkdownParser(md);
            Map<String, Object> fm1 = parser.getFrontMatter();
            fm1.put("newKey", "newValue");
            Map<String, Object> fm2 = parser.getFrontMatter();
            assertFalse(fm2.containsKey("newKey"));
        }
    }
}
