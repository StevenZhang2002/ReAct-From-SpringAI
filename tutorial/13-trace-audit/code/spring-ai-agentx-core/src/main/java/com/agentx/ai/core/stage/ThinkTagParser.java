package com.agentx.ai.core.stage;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code <think/>} 标签解析器。
 * <p>
 * 无状态工具类，将 LLM 流式输出的文本 chunk 拆分为思考内容和正常文本。
 * 支持跨 chunk 的标签状态追踪（通过 inThink 参数）。
 *
 * @author bigchui
 */
public final class ThinkTagParser {

    private static final String THINK_START = "<think";
    private static final String THINK_END = "</think";

    private ThinkTagParser() {
    }

    /**
     * 内容段。
     *
     * @param thinking 是否为思考内容
     * @param content  文本内容
     */
    public record Segment(boolean thinking, String content) {
    }

    /**
     * 解析结果。
     *
     * @param segments 拆分后的内容段列表
     * @param inThink  更新后的 think 标签内状态
     */
    public record ParseResult(List<Segment> segments, boolean inThink) {
    }

    /**
     * 解析一个文本 chunk。
     *
     * @param chunk   当前文本 chunk
     * @param inThink 上一个 chunk 结束时的 think 标签内状态
     * @return 解析结果
     */
    public static ParseResult parse(String chunk, boolean inThink) {
        if (chunk == null || chunk.isEmpty()) {
            return new ParseResult(List.of(), inThink);
        }

        List<Segment> segments = new ArrayList<>();
        boolean currentInThink = inThink;
        int index = 0;

        while (index < chunk.length()) {
            int thinkStartIdx = chunk.indexOf(THINK_START, index);
            int thinkEndIdx = chunk.indexOf(THINK_END, index);

            if (thinkStartIdx == -1 && thinkEndIdx == -1) {
                String remaining = chunk.substring(index);
                if (!remaining.isEmpty()) {
                    segments.add(new Segment(currentInThink, remaining));
                }
                break;
            }

            int nextTagPos;
            boolean isStartTag;

            if (thinkStartIdx != -1 && (thinkEndIdx == -1 || thinkStartIdx < thinkEndIdx)) {
                nextTagPos = thinkStartIdx;
                isStartTag = true;
            } else {
                nextTagPos = thinkEndIdx;
                isStartTag = false;
            }

            if (nextTagPos > index) {
                String beforeTag = chunk.substring(index, nextTagPos);
                if (!beforeTag.isEmpty()) {
                    segments.add(new Segment(currentInThink, beforeTag));
                }
            }

            int tagEnd = chunk.indexOf('>', nextTagPos);
            if (tagEnd == -1) {
                currentInThink = isStartTag;
                break;
            }

            currentInThink = isStartTag;
            index = tagEnd + 1;
        }

        return new ParseResult(segments, currentInThink);
    }

    /**
     * 去除文本中的 {@code <think>...</think>} 标签及其内容。
     */
    public static String stripThinkTags(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.replaceAll("(?s)<think[^>]*>.*?</think[^>]*>", "").trim();
    }
}
