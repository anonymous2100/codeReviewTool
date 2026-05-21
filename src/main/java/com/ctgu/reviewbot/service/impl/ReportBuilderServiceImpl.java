package com.ctgu.reviewbot.service.impl;

import com.ctgu.reviewbot.model.ReviewIssue;
import com.ctgu.reviewbot.model.ReviewReport;
import com.ctgu.reviewbot.model.ReviewResult;
import com.ctgu.reviewbot.model.Severity;
import com.ctgu.reviewbot.service.ReportBuilderService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ReportBuilderServiceImpl implements ReportBuilderService
{
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String buildMarkdownReport(ReviewReport report)
    {
        StringBuilder sb = new StringBuilder();
        IssueStats stats = computeStats(report.getResults());
        String riskText = report.hasBlockingIssue() ? "[安全]" : (stats.warning > 0 ? "[警告]" : "[低风险]");
        sb.append("## Code Review Bot 自动审查报告\n\n");
        sb.append("> **审查时间**: ").append(report.getReviewTime().format(DT_FMT));
        sb.append(" | **变更文件**: ").append(report.getReviewedFiles()).append(" 个(已审查)");
        if(report.getSkippedFiles() > 0)
        {
            sb.append(" / ").append(report.getSkippedFiles()).append(" 个(已跳过)");
        }
        sb.append("\n");
        sb.append("> **风险等级**: ").append(riskText);
        sb.append(" | **耗时**: ").append(formatDuration(report.getDurationMs()));
        sb.append(" | **Commit**: `").append(shortSha(report.getCommitSha())).append("`\n");
        sb.append("\n---\n\n");
        int fileNum = 0;
        for(ReviewResult result : report.getResults())
        {
            fileNum++;
            appendFileSection(sb, result, fileNum);
        }
        sb.append("\n> [提示] *本报告由 Code Review Bot 自动生成，仅供参考，最终 Review 决策以人工判断为准。*\n");
        log.info("Built markdown report: {} files, {} issues", report.getTotalFiles(), stats.total);
        return sb.toString();
    }

    private void appendFileSection(StringBuilder sb, ReviewResult result, int num)
    {
        String riskText = result.hasBlockingIssue() ? "[严重]" : (result.getWorstSeverity() == Severity.WARNING ? "[警告]" : "[建议]");
        sb.append("### ").append(riskText).append(" `").append(result.getFilePath()).append("`\n\n");
        appendIssueSection(sb, "[安全] 安全漏洞", result.getSecurityIssues());
        appendIssueSection(sb, "[性能] 性能隐患", result.getPerformanceIssues());
        appendIssueSection(sb, "[规范] 编码规范", result.getCodeStyleIssues());
        sb.append("#### 总体评价\n");
        sb.append("- ").append(result.getSummary()).append("\n");
        sb.append("\n---\n\n");
    }

    private void appendIssueSection(StringBuilder sb, String title, List<ReviewIssue> issues)
    {
        sb.append("##### ").append(title).append("\n");
        if(issues.isEmpty())
        {
            sb.append("- 无\n\n");
        }
        else
        {
            for(ReviewIssue issue : issues)
            {
                sb.append("- **[L").append(issue.getLineNumber()).append("]** ").append(severityText(issue.getSeverity())).append(" ")
                    .append(issue.getDescription()).append("\n");
                sb.append("  → **修复**: ").append(issue.getSuggestion()).append("\n\n");
            }
        }
    }

    private String severityText(Severity s)
    {
        return switch(s){
            case CRITICAL -> "[严重]";
            case WARNING -> "[警告]";
            case SUGGESTION -> "[建议]";
            default -> "[未知]";
        };
    }

    private IssueStats computeStats(List<ReviewResult> results)
    {
        int critical = 0, warning = 0, suggestion = 0;
        for(ReviewResult r : results)
        {
            critical += r.criticalCount();
            warning += r.warningCount();
            suggestion += r.suggestionCount();
        }
        return new IssueStats(critical, warning, suggestion, critical + warning + suggestion);
    }

    private String formatDuration(long ms)
    {
        if(ms < 1000)
        {
            return ms + "ms";
        }
        return String.format("%.1fs", ms / 1000.0);
    }

    private String shortSha(String sha)
    {
        if(sha == null)
        {
            return "unknown";
        }
        return sha.length() > 8 ? sha.substring(0, 8) : sha;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class IssueStats
    {
        private int critical;
        private int warning;
        private int suggestion;
        private int total;
    }
}
