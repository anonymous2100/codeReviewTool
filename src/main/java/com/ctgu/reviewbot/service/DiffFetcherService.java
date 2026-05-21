package com.ctgu.reviewbot.service;

import com.ctgu.reviewbot.model.FileDiff;

import java.util.List;

public interface DiffFetcherService
{
    List<FileDiff> fetchMrDiff(String projectId, Long mergeRequestIid);

    List<FileDiff> fetchCommitDiff(String projectId, String commitSha);
}
