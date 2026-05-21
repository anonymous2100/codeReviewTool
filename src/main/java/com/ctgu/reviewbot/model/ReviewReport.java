package com.ctgu.reviewbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一次 AI 审查的完整报告。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReport
{
    /**
     * GitLab 项目 ID
     */
    private String projectId;
    /**
     * MR IID（Push 时为 null）
     */
    private Long mergeRequestIid;
    /**
     * 审查的 commit SHA
     */
    private String commitSha;
    /**
     * 源分支
     */
    private String sourceBranch;
    /**
     * 目标分支
     */
    private String targetBranch;
    /**
     * 审查时间
     */
    private LocalDateTime reviewTime;
    /**
     * 各文件审查结果
     */
    private List<ReviewResult> results;
    /**
     * 总文件数
     */
    private int totalFiles;
    /**
     * 已审查文件数
     */
    private int reviewedFiles;
    /**
     * 跳过文件数
     */
    private int skippedFiles;
    /**
     * 总耗时 (ms)
     */
    private long durationMs;

    public boolean hasBlockingIssue()
    {
        return results.stream().anyMatch(ReviewResult::hasBlockingIssue);
    }

    public IssueCounts issueCounts()
    {
        int critical = 0, warning = 0, suggestion = 0;
        for(ReviewResult r : results)
        {
            critical += r.criticalCount();
            warning += r.warningCount();
            suggestion += r.suggestionCount();
        }
        return new IssueCounts(critical, warning, suggestion);
    }

    /**
     * Aggregated issue counts used by logging & metrics.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueCounts
    {
        /**
         * CRITICAL 数量
         */
        private int critical;
        /**
         * WARNING 数量
         */
        private int warning;
        /**
         * SUGGESTION 数量
         */
        private int suggestion;
    }
}
