package com.ctgu.reviewbot.service;

import com.ctgu.reviewbot.model.ReviewReport;

public interface GitLabCommentService
{
    void writeReviewComment(ReviewReport report);

    void writeFallbackComment(String projectId, Long mergeRequestIid, String errorMessage);
}
