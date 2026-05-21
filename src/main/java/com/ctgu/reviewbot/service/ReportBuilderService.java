package com.ctgu.reviewbot.service;

import com.ctgu.reviewbot.model.ReviewReport;

public interface ReportBuilderService
{
    String buildMarkdownReport(ReviewReport report);
}
