package com.ctgu.reviewbot.service.impl;

import com.ctgu.reviewbot.model.FileDiff;
import com.ctgu.reviewbot.model.ReviewIssue;
import com.ctgu.reviewbot.model.ReviewResult;
import com.ctgu.reviewbot.model.Severity;
import com.ctgu.reviewbot.monitoring.ReviewMetrics;
import com.ctgu.reviewbot.service.AiReviewService;
import com.ctgu.reviewbot.service.FileFilterService;
import com.ctgu.reviewbot.service.RetryHandler;
import com.ctgu.reviewbot.util.PromptBuilder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AiReviewServiceImpl implements AiReviewService
{
    private static final Pattern ISSUE_PATTERN = Pattern
        .compile("(\\[L[^\\]]*\\])\\s*(🔴|🟡|🟢)\\s*(.+?)\\s*→\\s*(.+)");
    private static final Pattern LINE_NUM_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern RISK_PATTERN = Pattern.compile("风险等级[：:]\\s*(.+)");
    private static final Pattern MERGE_PATTERN = Pattern.compile("是否建议合并[：:]\\s*(.+)");
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("一句话总结[：:]\\s*(.+)");
    private static final Pattern SECTION_PATTERN = Pattern.compile("###\\s*(.+)");

    private final ChatLanguageModel chatModel;
    private final RetryHandler retryHandler;
    private final PromptBuilder promptBuilder;
    private final FileFilterService fileFilterService;
    private final ReviewMetrics metrics;
    private final ExecutorService reviewExecutor;

    public AiReviewServiceImpl(ChatLanguageModel chatModel, RetryHandler retryHandler, PromptBuilder promptBuilder,
        FileFilterService fileFilterService, ReviewMetrics metrics, ExecutorService reviewExecutor)
    {
        this.chatModel = chatModel;
        this.retryHandler = retryHandler;
        this.promptBuilder = promptBuilder;
        this.fileFilterService = fileFilterService;
        this.metrics = metrics;
        this.reviewExecutor = reviewExecutor;
    }

    @Override
    public ReviewResult review(FileDiff fileDiff)
    {
        String prompt = promptBuilder.buildReviewPrompt(fileDiff);
        long start = System.currentTimeMillis();
        try
        {
            List<ChatMessage> messages = List.of(new SystemMessage(promptBuilder.getSystemPrompt()),
                new UserMessage(prompt));
            log.info("Sending request to LLM for file: {}, prompt length={} chars", fileDiff.getPath(),
                prompt.length());
            log.debug("LLM systemPrompt for {}: {}", fileDiff.getPath(), promptBuilder.getSystemPrompt().substring(0,
                Math.min(200, promptBuilder.getSystemPrompt().length())));
            log.debug("LLM userPrompt for {}: {}", fileDiff.getPath(),
                prompt.length() <= 2000 ? prompt : prompt.substring(0, 2000));
            Response<AiMessage> response = retryHandler.executeWithRetry(() -> chatModel.generate(messages),
                "review:" + fileDiff.getPath());
            long duration = System.currentTimeMillis() - start;
            metrics.recordApiCall(fileDiff.getPath(), duration, true);
            String content = response.content().text();
            log.info("Received response from LLM for file: {}, duration={}ms, response length={} chars",
                fileDiff.getPath(), duration, content != null ? content.length() : 0);
            log.debug("LLM response content for {}: {}", fileDiff.getPath(),
                content != null ? content.substring(0, Math.min(1000, content.length())) : "null");
            if(content == null || content.isBlank())
            {
                log.warn("Empty response from AI for file: {}", fileDiff.getPath());
                ReviewResult empty = ReviewResult.empty(fileDiff.getPath());
                empty.setDiffContent(fileDiff.getDiff());
                empty.setAddedLines(fileDiff.getAddedLines());
                empty.setRemovedLines(fileDiff.getRemovedLines());
                return empty;
            }
            ReviewResult result = parseReviewResponse(fileDiff.getPath(), content);
            result.setDiffContent(fileDiff.getDiff());
            result.setAddedLines(fileDiff.getAddedLines());
            result.setRemovedLines(fileDiff.getRemovedLines());
            return result;
        }
        catch(Exception e)
        {
            long duration = System.currentTimeMillis() - start;
            metrics.recordApiCall(fileDiff.getPath(), duration, false);
            log.error("AI review failed for file: {}", fileDiff.getPath(), e);
            return ReviewResult.failed(fileDiff.getPath(), e.getMessage());
        }
    }

    @Override
    public List<ReviewResult> reviewBatch(List<FileDiff> diffs, int maxConcurrency)
    {
        List<FileDiff> filtered = diffs.stream().filter(fileFilterService::shouldReview).toList();
        if(filtered.isEmpty())
        {
            log.info("No files to review after filtering, batch skipped");
            return List.of();
        }
        log.info("Starting batch review: {} files to review, maxConcurrency={}", filtered.size(), maxConcurrency);
        Semaphore semaphore = new Semaphore(maxConcurrency);
        List<CompletableFuture<ReviewResult>> futures = filtered.stream()
            .map(diff -> CompletableFuture.supplyAsync(() -> {
                semaphore.acquireUninterruptibly();
                try
                {
                    return review(diff);
                }
                finally
                {
                    semaphore.release();
                }
            }, reviewExecutor)).toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    @Override
    public ReviewResult parseReviewResponse(String filePath, String content)
    {
        List<ReviewIssue> securityIssues = new ArrayList<>();
        List<ReviewIssue> performanceIssues = new ArrayList<>();
        List<ReviewIssue> codeStyleIssues = new ArrayList<>();
        String currentSection = null;
        String[] lines = content.split("\n");
        for(String line : lines)
        {
            String trimmed = line.trim();
            if(trimmed.isEmpty())
            {
                continue;
            }
            Matcher sectionMatcher = SECTION_PATTERN.matcher(trimmed);
            if(sectionMatcher.matches())
            {
                currentSection = sectionMatcher.group(1).trim();
                continue;
            }
            if(trimmed.equals("无") || trimmed.startsWith("无"))
            {
                continue;
            }
            Matcher issueMatcher = ISSUE_PATTERN.matcher(trimmed);
            if(issueMatcher.find())
            {
                int lineNumber = parseLineNumber(issueMatcher.group(1));
                Severity severity = parseSeverity(issueMatcher.group(2));
                String description = issueMatcher.group(3).trim();
                String suggestion = issueMatcher.group(4).trim();
                String category = mapSectionToCategory(currentSection);
                ReviewIssue issue = new ReviewIssue(lineNumber, severity, category, description, suggestion);
                switch(category){
                    case "security" -> securityIssues.add(issue);
                    case "performance" -> performanceIssues.add(issue);
                    case "code_style" -> codeStyleIssues.add(issue);
                }
            }
        }
        String summary = extractSummary(content);
        Severity worstSeverity = computeWorstSeverity(securityIssues, performanceIssues, codeStyleIssues);
        boolean hasBlocking = worstSeverity.isBlocking();
        return new ReviewResult(filePath, securityIssues, performanceIssues, codeStyleIssues, summary, worstSeverity,
            hasBlocking, null, 0, 0);
    }

    private int parseLineNumber(String s)
    {
        Matcher m = LINE_NUM_PATTERN.matcher(s);
        if(m.find())
        {
            try
            {
                return Integer.parseInt(m.group(1));
            }
            catch(NumberFormatException e)
            {
                return 0;
            }
        }
        return 0;
    }

    private Severity parseSeverity(String icon)
    {
        return switch(icon){
            case "🔴" -> Severity.CRITICAL;
            case "🟡" -> Severity.WARNING;
            case "🟢" -> Severity.SUGGESTION;
            default -> Severity.WARNING;
        };
    }

    private String mapSectionToCategory(String section)
    {
        if(section == null)
        {
            return "code_style";
        }
        if(section.contains("安全"))
        {
            return "security";
        }
        if(section.contains("性能"))
        {
            return "performance";
        }
        if(section.contains("规范"))
        {
            return "code_style";
        }
        return "code_style";
    }

    private Severity computeWorstSeverity(List<ReviewIssue>... issueLists)
    {
        Severity worst = Severity.NONE;
        for(List<ReviewIssue> issues : issueLists)
        {
            for(ReviewIssue issue : issues)
            {
                if(issue.getSeverity().ordinal() > worst.ordinal())
                {
                    worst = issue.getSeverity();
                }
            }
        }
        return worst;
    }

    private String extractSummary(String content)
    {
        int idx = content.indexOf("总体评价");
        if(idx < 0)
        {
            return content.substring(0, Math.min(200, content.length()));
        }
        String section = content.substring(idx);
        Matcher riskMatcher = RISK_PATTERN.matcher(section);
        String risk = riskMatcher.find() ? riskMatcher.group(1).trim() : "未知";
        Matcher mergeMatcher = MERGE_PATTERN.matcher(section);
        String merge = mergeMatcher.find() ? mergeMatcher.group(1).trim() : "未知";
        Matcher summaryMatcher = SUMMARY_PATTERN.matcher(section);
        String summary = summaryMatcher.find() ? summaryMatcher.group(1).trim() : "";
        return String.format("风险: %s | 建议合并: %s | %s", risk, merge, summary);
    }
}
