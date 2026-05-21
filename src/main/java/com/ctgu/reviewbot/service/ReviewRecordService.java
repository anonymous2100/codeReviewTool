package com.ctgu.reviewbot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctgu.reviewbot.model.ReviewRecord;
import com.ctgu.reviewbot.model.ReviewReport;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public interface ReviewRecordService
{
    ReviewRecord save(ReviewReport report);

    Page<ReviewRecord> findByProject(String projectId, int page, int size);

    List<ReviewRecord> findByProjectAndDateRange(String projectId, LocalDate from, LocalDate to);

    ReviewStats getStats(String projectId, LocalDate from, LocalDate to);

    ReviewRecord findById(Long id);

    List<ReviewRecord> getTrend(String projectId, int days);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class ReviewStats
    {
        private String projectId;
        private LocalDate from;
        private LocalDate to;
        private int totalReviews;
        private int blockingReviews;
        private int totalCritical;
        private int totalWarning;
        private int totalSuggestion;
        private long avgDurationMs;
    }
}
