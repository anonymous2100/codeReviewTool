package com.ctgu.reviewbot.service.impl;

import com.ctgu.reviewbot.client.GitLabClient;
import com.ctgu.reviewbot.model.FileDiff;
import com.ctgu.reviewbot.service.DiffFetcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DiffFetcherServiceImpl implements DiffFetcherService
{
    private final GitLabClient gitLabClient;

    public DiffFetcherServiceImpl(GitLabClient gitLabClient)
    {
        this.gitLabClient = gitLabClient;
    }

    @Override
    public List<FileDiff> fetchMrDiff(String projectId, Long mergeRequestIid)
    {
        log.info("Fetching MR diff: project={}, mergeRequestIid={}", projectId, mergeRequestIid);
        List<FileDiff.GitLabDiffDto> dtos = gitLabClient.getMrDiffs(projectId, mergeRequestIid);
        List<FileDiff> diffs = dtos.stream().map(FileDiff::fromGitLabDiff).toList();
        log.info("Fetched {} files from MR", diffs.size());
        return diffs;
    }

    @Override
    public List<FileDiff> fetchCommitDiff(String projectId, String commitSha)
    {
        log.info("Fetching commit diff: project={}, sha={}", projectId, commitSha);
        List<FileDiff.GitLabDiffDto> dtos = gitLabClient.getCommitDiff(projectId, commitSha);
        return dtos.stream().map(FileDiff::fromGitLabDiff).toList();
    }
}
