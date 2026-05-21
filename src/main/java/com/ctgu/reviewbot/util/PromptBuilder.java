package com.ctgu.reviewbot.util;

import com.ctgu.reviewbot.config.ReviewProperties;
import com.ctgu.reviewbot.model.FileDiff;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Prompt 构建器，组装系统提示词和文件审查提示模板
 */
@Slf4j
@Component
public class PromptBuilder
{
    /**
     * Diff 中 hunk 头部的正则匹配模式，用于解析变更范围
     */
    private static final Pattern HUNK_HEADER = Pattern.compile("^@@ -(\\d+),?(\\d*) \\+(\\d+),?(\\d*) @@.*$");
    /**
     * hunk 截断时保留的上下文行数
     */
    private static final int CONTEXT_LINES = 15;
    /**
     * 审查配置（含 diff 最大字符数等）
     */
    private final ReviewProperties reviewProperties;
    /**
     * 系统提示词缓存（volatile 实现双重检查锁定的可见性）
     */
    private volatile String systemPromptCache;
    /**
     * 审查模板缓存（volatile 实现双重检查锁定的可见性）
     */
    private volatile String reviewTemplateCache;

    public PromptBuilder(ReviewProperties reviewProperties)
    {
        this.reviewProperties = reviewProperties;
    }

    public String getSystemPrompt()
    {
        if(systemPromptCache == null)
        {
            synchronized(this)
            {
                if(systemPromptCache == null)
                {
                    systemPromptCache = loadResource("prompts/system-prompt.txt");
                }
            }
        }
        return systemPromptCache;
    }

    public String buildReviewPrompt(FileDiff diff)
    {
        if(reviewTemplateCache == null)
        {
            synchronized(this)
            {
                if(reviewTemplateCache == null)
                {
                    reviewTemplateCache = loadResource("prompts/review-template.txt");
                }
            }
        }
        return reviewTemplateCache.replace("{filePath}", diff.getPath())
            .replace("{addedLines}", String.valueOf(diff.getAddedLines()))
            .replace("{removedLines}", String.valueOf(diff.getRemovedLines()))
            .replace("{diffContent}", truncateDiff(diff.getDiff()));
    }

    String truncateDiff(String diff)
    {
        int maxChars = reviewProperties.getMaxDiffChars();
        if(diff == null || diff.isEmpty())
        {
            return "(empty diff)";
        }
        if(diff.length() <= maxChars)
        {
            return diff;
        }
        List<HunkRange> hunkRanges = extractHunkRanges(diff);
        // Sort by size descending — keep the largest hunks
        hunkRanges.sort(Comparator.comparingInt(HunkRange::getChangeCount).reversed());
        StringBuilder result = new StringBuilder();
        result.append("[Diff 已截断, 原始大小: ").append(diff.length()).append(" 字符]\n\n");
        int remaining = maxChars - result.length() - 100; // reserve for header
        String[] lines = diff.split("\n", -1);
        for(HunkRange hunk : hunkRanges)
        {
            if(remaining <= 0)
            {
                break;
            }
            int start = Math.max(0, hunk.getStart() - CONTEXT_LINES);
            int end = Math.min(lines.length, hunk.getEnd() + CONTEXT_LINES);
            for(int i = start; i < end && remaining > 0; i++)
            {
                String line = lines[i];
                result.append(line).append('\n');
                remaining -= line.length() + 1;
            }
            result.append("...\n");
        }
        result.append("\n[截断结束]");
        return result.toString();
    }

    private List<HunkRange> extractHunkRanges(String diff)
    {
        List<HunkRange> ranges = new ArrayList<>();
        String[] lines = diff.split("\n", -1);
        int currentStart = -1;
        int changeCount = 0;
        for(int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            Matcher m = HUNK_HEADER.matcher(line);
            if(m.matches())
            {
                if(currentStart >= 0 && changeCount > 0)
                {
                    ranges.add(new HunkRange(currentStart, i, changeCount));
                }
                currentStart = i;
                changeCount = 0;
            }
            if(currentStart >= 0 && (line.startsWith("+") || line.startsWith("-")))
            {
                changeCount++;
            }
        }
        // Last hunk
        if(currentStart >= 0 && changeCount > 0)
        {
            ranges.add(new HunkRange(currentStart, lines.length, changeCount));
        }
        return ranges;
    }

    private String loadResource(String path)
    {
        try
        {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        }
        catch(IOException e)
        {
            log.error("Failed to load resource: {}", path, e);
            throw new RuntimeException("Failed to load prompt template: " + path, e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class HunkRange
    {
        /**
         * hunk 在 diff 行数组中的起始行号
         */
        private int start;
        /**
         * hunk 在 diff 行数组中的结束行号
         */
        private int end;
        /**
         * hunk 中包含的变更行数（+/- 行）
         */
        private int changeCount;
    }
}
