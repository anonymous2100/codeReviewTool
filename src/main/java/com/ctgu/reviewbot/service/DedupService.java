package com.ctgu.reviewbot.service;

public interface DedupService
{
    boolean isDuplicate(String projectId, String commitSha);

    void markAsReviewed(String projectId, String commitSha);
}
