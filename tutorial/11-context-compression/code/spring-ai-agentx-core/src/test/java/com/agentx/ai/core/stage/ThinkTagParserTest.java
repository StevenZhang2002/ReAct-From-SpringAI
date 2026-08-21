package com.agentx.ai.core.stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ThinkTagParser 单元测试（第08节）。
 *
 * <p>测试 think 标签解析、跨 chunk 状态追踪、标签剥离。
 */
@DisplayName("ThinkTagParser 测试")
class ThinkTagParserTest {

    @Nested
    @DisplayName("parse() 方法测试")
    class ParseTests {

        @Test
        @DisplayName("解析空字符串")
        void shouldParseEmptyString() {
            // When
            ThinkTagParser.ParseResult result = ThinkTagParser.parse("", false);

            // Then
            assertTrue(result.segments().isEmpty());
            assertFalse(result.inThink());
        }

        @Test
        @DisplayName("解析 null")
        void shouldParseNull() {
            // When
            ThinkTagParser.ParseResult result = ThinkTagParser.parse(null, false);

            // Then
            assertTrue(result.segments().isEmpty());
            assertFalse(result.inThink());
        }

        @Test
        @DisplayName("解析普通文本")
        void shouldParsePlainText() {
            // When
            ThinkTagParser.ParseResult result = ThinkTagParser.parse("你好世界", false);

            // Then
            assertEquals(1, result.segments().size());
            assertFalse(result.segments().get(0).thinking());
            assertEquals("你好世界", result.segments().get(0).content());
            assertFalse(result.inThink());
        }

        @Test
        @DisplayName("解析带 think 标签的文本")
        void shouldParseWithThinkTags() {
            // Given
            String input = "<think>思考内容</think>正常文本";

            // When
            ThinkTagParser.ParseResult result = ThinkTagParser.parse(input, false);

            // Then
            assertEquals(2, result.segments().size());
            assertTrue(result.segments().get(0).thinking());
            assertEquals("思考内容", result.segments().get(0).content());
            assertFalse(result.segments().get(1).thinking());
            assertEquals("正常文本", result.segments().get(1).content());
        }

        @Test
        @DisplayName("跨 chunk 状态追踪 - 开始标签")
        void shouldTrackStateAcrossChunks_StartTag() {
            // Given - 第一个 chunk 以 <think> 开始但没有结束
            String chunk1 = "<think>思考中";

            // When
            ThinkTagParser.ParseResult result1 = ThinkTagParser.parse(chunk1, false);

            // Then
            assertTrue(result1.inThink());
        }

        @Test
        @DisplayName("跨 chunk 状态追踪 - 结束标签")
        void shouldTrackStateAcrossChunks_EndTag() {
            // Given - 第二个 chunk 继续思考内容并结束
            String chunk2 = "继续思考</think>正常文本";

            // When - 假设上一个 chunk 结束时 inThink = true
            ThinkTagParser.ParseResult result = ThinkTagParser.parse(chunk2, true);

            // Then
            assertFalse(result.inThink());
        }

        @Test
        @DisplayName("解析多个 think 标签")
        void shouldParseMultipleThinkTags() {
            // Given
            String input = "<think>思考1</think>文本1<think>思考2</think>文本2";

            // When
            ThinkTagParser.ParseResult result = ThinkTagParser.parse(input, false);

            // Then
            assertEquals(4, result.segments().size());
            assertTrue(result.segments().get(0).thinking());
            assertEquals("思考1", result.segments().get(0).content());
            assertFalse(result.segments().get(1).thinking());
            assertEquals("文本1", result.segments().get(1).content());
            assertTrue(result.segments().get(2).thinking());
            assertEquals("思考2", result.segments().get(2).content());
            assertFalse(result.segments().get(3).thinking());
            assertEquals("文本2", result.segments().get(3).content());
        }

        @Test
        @DisplayName("解析带属性的 think 标签")
        void shouldParseThinkTagWithAttributes() {
            // Given
            String input = "<think type=\"reasoning\">思考内容</think>正常文本";

            // When
            ThinkTagParser.ParseResult result = ThinkTagParser.parse(input, false);

            // Then
            assertEquals(2, result.segments().size());
            assertTrue(result.segments().get(0).thinking());
            assertFalse(result.segments().get(1).thinking());
        }
    }

    @Nested
    @DisplayName("stripThinkTags() 方法测试")
    class StripThinkTagsTests {

        @Test
        @DisplayName("剥离 think 标签")
        void shouldStripThinkTags() {
            // Given
            String input = "<think>思考内容</think>正常文本";

            // When
            String result = ThinkTagParser.stripThinkTags(input);

            // Then
            assertEquals("正常文本", result);
        }

        @Test
        @DisplayName("剥离多个 think 标签")
        void shouldStripMultipleThinkTags() {
            // Given
            String input = "<think>思考1</think>文本1<think>思考2</think>文本2";

            // When
            String result = ThinkTagParser.stripThinkTags(input);

            // Then
            assertEquals("文本1文本2", result);
        }

        @Test
        @DisplayName("没有 think 标签时返回原文本")
        void shouldReturnOriginalWhenNoThinkTags() {
            // Given
            String input = "普通文本";

            // When
            String result = ThinkTagParser.stripThinkTags(input);

            // Then
            assertEquals("普通文本", result);
        }

        @Test
        @DisplayName("处理 null 输入")
        void shouldHandleNull() {
            // When
            String result = ThinkTagParser.stripThinkTags(null);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("处理空字符串")
        void shouldHandleEmptyString() {
            // When
            String result = ThinkTagParser.stripThinkTags("");

            // Then
            assertEquals("", result);
        }

        @Test
        @DisplayName("剥离带属性的 think 标签")
        void shouldStripThinkTagsWithAttributes() {
            // Given
            String input = "<think type=\"reasoning\">思考内容</think>正常文本";

            // When
            String result = ThinkTagParser.stripThinkTags(input);

            // Then
            assertEquals("正常文本", result);
        }
    }

    @Nested
    @DisplayName("Segment record 测试")
    class SegmentTests {

        @Test
        @DisplayName("创建思考段")
        void shouldCreateThinkingSegment() {
            // When
            ThinkTagParser.Segment segment = new ThinkTagParser.Segment(true, "思考内容");

            // Then
            assertTrue(segment.thinking());
            assertEquals("思考内容", segment.content());
        }

        @Test
        @DisplayName("创建普通文本段")
        void shouldCreateNormalSegment() {
            // When
            ThinkTagParser.Segment segment = new ThinkTagParser.Segment(false, "普通文本");

            // Then
            assertFalse(segment.thinking());
            assertEquals("普通文本", segment.content());
        }

        @Test
        @DisplayName("Segment 相等性")
        void shouldBeEqualForSameValues() {
            // Given
            ThinkTagParser.Segment segment1 = new ThinkTagParser.Segment(true, "内容");
            ThinkTagParser.Segment segment2 = new ThinkTagParser.Segment(true, "内容");

            // Then
            assertEquals(segment1, segment2);
        }
    }

    @Nested
    @DisplayName("ParseResult record 测试")
    class ParseResultTests {

        @Test
        @DisplayName("创建解析结果")
        void shouldCreateParseResult() {
            // Given
            List<ThinkTagParser.Segment> segments = List.of(
                    new ThinkTagParser.Segment(true, "思考"),
                    new ThinkTagParser.Segment(false, "文本")
            );

            // When
            ThinkTagParser.ParseResult result = new ThinkTagParser.ParseResult(segments, false);

            // Then
            assertEquals(2, result.segments().size());
            assertFalse(result.inThink());
        }
    }
}
