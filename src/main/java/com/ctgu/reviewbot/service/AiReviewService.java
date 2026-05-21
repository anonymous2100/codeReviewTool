package com.ctgu.reviewbot.service;

import com.ctgu.reviewbot.model.FileDiff;
import com.ctgu.reviewbot.model.ReviewResult;

import java.util.List;

public interface AiReviewService
{
    ReviewResult review(FileDiff fileDiff);

    List<ReviewResult> reviewBatch(List<FileDiff> diffs, int maxConcurrency);

    ReviewResult parseReviewResponse(String filePath, String content);
}
