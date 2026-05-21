package com.ctgu.reviewbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单个文件的 AI 审查结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResult
{
    /**
     * 被审查文件路径
     */
    private String filePath;
    /**
     * 安全问题列表
     */
    private List<ReviewIssue> securityIssues;
    /**
     * 性能问题列表
     */
    private List<ReviewIssue> performanceIssues;
    /**
     * 编码规范问题列表
     */
    private List<ReviewIssue> codeStyleIssues;
    /**
     * 总体评价
     */
    private String summary;
    /**
     * 最严重的等级
     */
    private Severity worstSeverity;
    /**
     * 是否存在阻塞性 (CRITICAL) 问题
     */
    private boolean hasBlockingIssue;
    /**
     * 文件 diff 内容（用于前端展示代码变更）
     */
    private String diffContent;
    /**
     * 新增行数
     */
    private int addedLines;
    /**
     * 删除行数
     */
    private int removedLines;

    public static ReviewResult failed(String filePath, String error)
    {
        return new ReviewResult(filePath, List.of(), List.of(), List.of(), "AI 分析失败: " + error, Severity.NONE, false,
            null, 0, 0);
    }

    public static ReviewResult empty(String filePath)
    {
        return new ReviewResult(filePath, List.of(), List.of(), List.of(), "无问题", Severity.NONE, false, null, 0, 0);
    }

    public boolean isFailure()
    {
        return summary != null && summary.startsWith("AI 分析失败");
    }

    public boolean hasBlockingIssue()
    {
        return hasBlockingIssue;
    }

    public int totalIssueCount()
    {
        return securityIssues.size() + performanceIssues.size() + codeStyleIssues.size();
    }

    public int criticalCount()
    {
        return countBySeverity(Severity.CRITICAL);
    }

    public int warningCount()
    {
        return countBySeverity(Severity.WARNING);
    }

    public int suggestionCount()
    {
        return countBySeverity(Severity.SUGGESTION);
    }

    private int countBySeverity(Severity severity)
    {
        int count = 0;
        for(ReviewIssue issue : securityIssues)
        {
            if(issue.getSeverity() == severity)
            {
                count++;
            }
        }
        for(ReviewIssue issue : performanceIssues)
        {
            if(issue.getSeverity() == severity)
            {
                count++;
            }
        }
        for(ReviewIssue issue : codeStyleIssues)
        {
            if(issue.getSeverity() == severity)
            {
                count++;
            }
        }
        return count;
    }
}
