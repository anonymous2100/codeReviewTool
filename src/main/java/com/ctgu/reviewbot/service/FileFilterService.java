package com.ctgu.reviewbot.service;

import com.ctgu.reviewbot.model.FileDiff;

public interface FileFilterService
{
    boolean shouldReview(FileDiff diff);
}
