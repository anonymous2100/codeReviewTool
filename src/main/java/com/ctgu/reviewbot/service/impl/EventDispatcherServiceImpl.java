package com.ctgu.reviewbot.service.impl;

import com.ctgu.reviewbot.config.ReviewProperties;
import com.ctgu.reviewbot.model.FileDiff;
import com.ctgu.reviewbot.model.ReviewReport;
import com.ctgu.reviewbot.model.ReviewResult;
import com.ctgu.reviewbot.model.WebhookEvent;
import com.ctgu.reviewbot.monitoring.ReviewMetrics;
import com.ctgu.reviewbot.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class EventDispatcherServiceImpl implements EventDispatcherService
{
    private final DedupService dedupService;
    private final DiffFetcherService diffFetcherService;
    private final FileFilterService fileFilterService;
    private final AiReviewService aiReviewService;
    private final ReportBuilderService reportBuilderService;
    private final GitLabCommentService gitLabCommentService;
    private final ReviewRecordService reviewRecordService;
    private final ReviewProperties reviewProperties;
    private final ReviewMetrics metrics;

    public EventDispatcherServiceImpl(DedupService dedupService, DiffFetcherService diffFetcherService,
        FileFilterService fileFilterService, AiReviewService aiReviewService,
        ReportBuilderService reportBuilderService, GitLabCommentService gitLabCommentService,
        ReviewRecordService reviewRecordService, ReviewProperties reviewProperties, ReviewMetrics metrics)
    {
        this.dedupService = dedupService;
        this.diffFetcherService = diffFetcherService;
        this.fileFilterService = fileFilterService;
        this.aiReviewService = aiReviewService;
        this.reportBuilderService = reportBuilderService;
        this.gitLabCommentService = gitLabCommentService;
        this.reviewRecordService = reviewRecordService;
        this.reviewProperties = reviewProperties;
        this.metrics = metrics;
    }

    @Async
    @Override
    public void handlePush(WebhookEvent event)
    {
        log.info("Handling push event: project={}, sha={}", event.projectId(), event.commitSha());
        processReview(event.projectId(), null, event.commitSha(), event.sourceBranch(), event.targetBranch());
    }

    @Override
    public void handlePushRaw(String projectId, String commitSha, String branch)
    {
        log.info("Handling raw push event: project={}, sha={}, branch={}", projectId, commitSha, branch);
        processReview(projectId, null, commitSha, branch, branch);
    }

    @Async
    @Override
    public void handleMergeRequest(WebhookEvent event)
    {
        log.info("Handling MR event: project={}, mergeRequestIid={}", event.projectId(), event.mergeRequestIid());
        String commitSha = event.commitSha();
        if(commitSha == null)
        {
            log.warn("No commit SHA in MR event, skipping");
            return;
        }
        if(dedupService.isDuplicate(event.projectId(), commitSha))
        {
            log.info("Duplicate MR event, skipping: project={}, sha={}", event.projectId(), commitSha);
            return;
        }
        try
        {
            processReview(event.projectId(), event.mergeRequestIid(), commitSha, event.sourceBranch(),
                event.targetBranch());
        }
        catch(AiServiceUnavailableException e)
        {
            log.error("AI service unavailable, posting fallback comment", e);
            try
            {
                gitLabCommentService.writeFallbackComment(event.projectId(), event.mergeRequestIid(), e.getMessage());
            }
            catch(Exception ex)
            {
                log.error("Failed to post fallback comment", ex);
            }
            throw e;
        }
    }

    private void processReview(String projectId, Long mergeRequestIid, String commitSha, String sourceBranch,
        String targetBranch)
    {
        long startTime = System.currentTimeMillis();
        metrics.incrementInProgress();
        try
        {
            List<FileDiff> allDiffs;
            if(mergeRequestIid != null)
            {
                allDiffs = diffFetcherService.fetchMrDiff(projectId, mergeRequestIid);
            }
            else
            {
                allDiffs = diffFetcherService.fetchCommitDiff(projectId, commitSha);
            }
            log.info("Fetched {} file diffs for project={} mr={}", allDiffs.size(), projectId, mergeRequestIid);
            if(allDiffs.size() > reviewProperties.getMaxFilesPerMr())
            {
                log.warn("Too many files ({}) in MR, limiting to {}", allDiffs.size(),
                    reviewProperties.getMaxFilesPerMr());
                allDiffs = allDiffs.subList(0, reviewProperties.getMaxFilesPerMr());
            }
            List<FileDiff> toReview = allDiffs.stream().filter(fileFilterService::shouldReview).toList();
            int skippedCount = allDiffs.size() - toReview.size();
            List<ReviewResult> results = aiReviewService.reviewBatch(allDiffs, reviewProperties.getMaxConcurrency());
            long durationMs = System.currentTimeMillis() - startTime;
            ReviewReport report = new ReviewReport(projectId, mergeRequestIid, commitSha, sourceBranch, targetBranch,
                LocalDateTime.now(), results, allDiffs.size(), toReview.size(), skippedCount, durationMs);
            reviewRecordService.save(report);
            gitLabCommentService.writeReviewComment(report);
            ReviewReport.IssueCounts counts = report.issueCounts();
            log.info(
                "review_completed project={} mr={} sha={} files={} reviewed={} "
                    + "skipped={} critical={} warning={} suggestion={} durationMs={} status={}",
                projectId, mergeRequestIid, commitSha, report.getTotalFiles(), report.getReviewedFiles(),
                report.getSkippedFiles(), counts.getCritical(), counts.getWarning(), counts.getSuggestion(),
                report.getDurationMs(), report.hasBlockingIssue() ? "BLOCKED" : "PASSED");
            metrics.recordReviewCompleted(report.hasBlockingIssue() ? "blocked" : "passed", report.getTotalFiles(),
                report.getDurationMs());
        }
        catch(Exception e)
        {
            log.error("Review process failed: project={}, mr={}", projectId, mergeRequestIid, e);
            metrics.recordReviewCompleted("failed", 0, System.currentTimeMillis() - startTime);
            throw e;
        }
        finally
        {
            metrics.decrementInProgress();
        }
    }

    @Override
    public boolean shouldTriggerReview(WebhookEvent event)
    {
        if(!event.isOpenMergeRequest())
        {
            return false;
        }
        boolean branchMatch = reviewProperties.getTargetBranches().stream().anyMatch(pattern -> {
            String target = event.targetBranch();
            if(target == null)
            {
                return false;
            }
            if(pattern.equals(target))
            {
                return true;
            }
            if(pattern.contains("*"))
            {
                try
                {
                    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                    return matcher.matches(Paths.get(target));
                }
                catch(Exception e)
                {
                    return false;
                }
            }
            return false;
        });
        if(!branchMatch)
        {
            log.info("MR target branch '{}' not in target-branches list, skipping", event.targetBranch());
        }
        return branchMatch;
    }
}
