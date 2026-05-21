package com.ctgu.reviewbot.util;

import com.ctgu.reviewbot.model.FileDiff;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 unified diff 内容为结构化 FileDiff 对象，支持多文件 diff 和行数统计
 */
@Slf4j
public final class DiffParser
{
    // git diff header: diff --git a/<old> b/<new>
    private static final Pattern DIFF_GIT_HEADER = Pattern.compile("^diff --git a/(.+) b/(.+)$", Pattern.MULTILINE);
    // hunk header: @@ -oldStart[,oldCount] +newStart[,newCount] @@
    private static final Pattern HUNK_HEADER = Pattern.compile("^@@ -(\\d+),?(\\d*) \\+(\\d+),?(\\d*) @@",
        Pattern.MULTILINE);

    private DiffParser()
    {
    }

    /**
     * Parses a multi-file unified diff string into individual FileDiff records.
     */
    public static List<FileDiff> parseMultiFileDiff(String diffContent)
    {
        List<FileDiff> diffs = new ArrayList<>();
        if(diffContent == null || diffContent.isBlank())
        {
            return diffs;
        }
        String[] parts = diffContent.split("(?m)(?=^diff --git )", -1);
        for(String part : parts)
        {
            if(part.isBlank())
            {
                continue;
            }
            FileDiff diff = parseSingleFileDiff(part);
            if(diff != null)
            {
                diffs.add(diff);
            }
        }
        return diffs;
    }

    /**
     * Parses a single file's unified diff block.
     */
    public static FileDiff parseSingleFileDiff(String diffBlock)
    {
        String[] lines = diffBlock.split("\n", -1);
        if(lines.length == 0)
        {
            return null;
        }
        String filePath = null;
        int addedLines = 0;
        int removedLines = 0;
        boolean isNew = false;
        boolean isDeleted = false;
        for(String line : lines)
        {
            Matcher headerMatch = DIFF_GIT_HEADER.matcher(line);
            if(headerMatch.matches())
            {
                filePath = headerMatch.group(2); // new file path
                if("/dev/null".equals(headerMatch.group(1)))
                {
                    isNew = true;
                }
                if("/dev/null".equals(headerMatch.group(2)))
                {
                    isDeleted = true;
                }
                continue;
            }
            // Track new file mode
            if(line.startsWith("new file mode"))
            {
                isNew = true;
                continue;
            }
            if(line.startsWith("deleted file mode"))
            {
                isDeleted = true;
                continue;
            }
            // Count added/removed lines (skip diff metadata lines)
            if(line.startsWith("+") && !line.startsWith("+++"))
            {
                addedLines++;
            }
            else if(line.startsWith("-") && !line.startsWith("---"))
            {
                removedLines++;
            }
        }
        if(filePath == null)
        {
            // Try to extract from "--- a/" or "+++ b/" lines
            for(String line : lines)
            {
                if(line.startsWith("+++ b/"))
                {
                    filePath = line.substring(6);
                    break;
                }
            }
        }
        if(filePath == null)
        {
            log.info("Could not determine file path from diff block");
            return null;
        }
        return new FileDiff(filePath, diffBlock, isNew, isDeleted, addedLines, removedLines);
    }

    /**
     * Extracts line number mappings from hunk headers for accurate issue location reporting. Returns an array of [oldStart, oldCount,
     * newStart, newCount] for each hunk.
     */
    public static List<HunkInfo> extractHunks(String diffContent)
    {
        List<HunkInfo> hunks = new ArrayList<>();
        if(diffContent == null)
        {
            return hunks;
        }
        for(String line : diffContent.split("\n", -1))
        {
            Matcher m = HUNK_HEADER.matcher(line);
            if(m.find())
            {
                int oldStart = Integer.parseInt(m.group(1));
                int oldCount = m.group(2).isEmpty() ? 1 : Integer.parseInt(m.group(2));
                int newStart = Integer.parseInt(m.group(3));
                int newCount = m.group(4).isEmpty() ? 1 : Integer.parseInt(m.group(4));
                hunks.add(new HunkInfo(oldStart, oldCount, newStart, newCount));
            }
        }
        return hunks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HunkInfo
    {
        private int oldStart;
        private int oldCount;
        private int newStart;
        private int newCount;
    }
}
