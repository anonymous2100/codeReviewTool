package com.ctgu.reviewbot.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审查记录实体 — 对应 review_records 表。
 */
@Data
@TableName("review_records")
public class ReviewRecord
{
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private String projectId;

    @TableField("mr_iid")
    private Long mergeRequestIid;

    @TableField("commit_sha")
    private String commitSha;

    @TableField("source_branch")
    private String sourceBranch;

    @TableField("target_branch")
    private String targetBranch;

    @TableField("total_files")
    private int totalFiles;

    @TableField("reviewed_files")
    private int reviewedFiles;

    @TableField("skipped_files")
    private int skippedFiles;

    @TableField("critical_count")
    private int criticalCount;

    @TableField("warning_count")
    private int warningCount;

    @TableField("suggestion_count")
    private int suggestionCount;

    @TableField("duration_ms")
    private long durationMs;

    @TableField("has_blocking")
    private boolean hasBlocking;

    @TableField("review_status")
    private ReviewStatus reviewStatus;

    @TableField("error_message")
    private String errorMessage;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<ReviewFileDetail> fileDetails = new ArrayList<>();

    public static ReviewRecord fromReport(ReviewReport report)
    {
        ReviewRecord record = new ReviewRecord();
        record.projectId = report.getProjectId();
        record.mergeRequestIid = report.getMergeRequestIid();
        record.commitSha = report.getCommitSha();
        record.sourceBranch = report.getSourceBranch();
        record.targetBranch = report.getTargetBranch();
        record.totalFiles = report.getTotalFiles();
        record.reviewedFiles = report.getReviewedFiles();
        record.skippedFiles = report.getSkippedFiles();
        ReviewReport.IssueCounts counts = report.issueCounts();
        record.criticalCount = counts.getCritical();
        record.warningCount = counts.getWarning();
        record.suggestionCount = counts.getSuggestion();
        record.durationMs = report.getDurationMs();
        record.hasBlocking = report.hasBlockingIssue();
        record.reviewStatus = ReviewStatus.COMPLETED;
        return record;
    }

}
