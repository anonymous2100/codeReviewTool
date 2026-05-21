package com.ctgu.reviewbot.service.impl;

import com.ctgu.reviewbot.client.GitLabClient;
import com.ctgu.reviewbot.model.ReviewReport;
import com.ctgu.reviewbot.service.GitLabCommentService;
import com.ctgu.reviewbot.service.ReportBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class GitLabCommentServiceImpl implements GitLabCommentService
{
    private final GitLabClient gitLabClient;
    private final ReportBuilderService reportBuilderService;

    public GitLabCommentServiceImpl(GitLabClient gitLabClient, ReportBuilderService reportBuilderService)
    {
        this.gitLabClient = gitLabClient;
        this.reportBuilderService = reportBuilderService;
    }

    @Override
    public void writeReviewComment(ReviewReport report)
    {
        String markdown = reportBuilderService.buildMarkdownReport(report);
        if(report.getMergeRequestIid() != null)
        {
            gitLabClient.createMrNote(report.getProjectId(), report.getMergeRequestIid(), markdown);
            log.info("Review comment posted to MR: project={}, mergeRequestIid={}", report.getProjectId(),
                report.getMergeRequestIid());
        }
        else if(report.getCommitSha() != null)
        {
            gitLabClient.createCommitComment(report.getProjectId(), report.getCommitSha(), markdown);
            log.info("Review comment posted to commit: project={}, sha={}", report.getProjectId(),
                report.getCommitSha());
        }
        else
        {
            log.warn("No target for review comment (no MR IID or commit SHA)");
        }
    }

    @Override
    public void writeFallbackComment(String projectId, Long mergeRequestIid, String errorMessage)
    {
        String template = loadFallbackTemplate();
        String fallbackNote = String.format(template, java.time.LocalDateTime.now(), errorMessage);
        gitLabClient.createMrNote(projectId, mergeRequestIid, fallbackNote);
        log.info("Fallback comment posted to MR: project={}, mergeRequestIid={}", projectId, mergeRequestIid);
    }

    private String loadFallbackTemplate()
    {
        try
        {
            return new ClassPathResource("prompts/fallback-comment.txt").getContentAsString(StandardCharsets.UTF_8);
        }
        catch(IOException e)
        {
            log.error("Failed to load fallback comment template", e);
            throw new RuntimeException("Failed to load fallback-comment.txt", e);
        }
    }
}
