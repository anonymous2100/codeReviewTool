package com.ctgu.reviewbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctgu.reviewbot.mapper.ReviewFileDetailMapper;
import com.ctgu.reviewbot.mapper.ReviewRecordMapper;
import com.ctgu.reviewbot.model.*;
import com.ctgu.reviewbot.service.ReviewRecordService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
public class ReviewRecordServiceImpl implements ReviewRecordService
{
    private final ReviewRecordMapper reviewRecordMapper;
    private final ReviewFileDetailMapper fileDetailMapper;
    private final ObjectMapper objectMapper;

    public ReviewRecordServiceImpl(ReviewRecordMapper reviewRecordMapper, ReviewFileDetailMapper fileDetailMapper,
        ObjectMapper objectMapper)
    {
        this.reviewRecordMapper = reviewRecordMapper;
        this.fileDetailMapper = fileDetailMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Override
    public ReviewRecord save(ReviewReport report)
    {
        ReviewRecord record = ReviewRecord.fromReport(report);
        record.setCreatedAt(LocalDateTime.now());
        reviewRecordMapper.insert(record);
        for(ReviewResult result : report.getResults())
        {
            ReviewFileDetail detail = new ReviewFileDetail();
            detail.setReviewId(record.getId());
            detail.setFilePath(result.getFilePath());
            detail.setAddedLines(result.getAddedLines());
            detail.setRemovedLines(result.getRemovedLines());
            detail.setSkipped(result.isFailure());
            if(result.isFailure())
            {
                detail.setSkipReason("AI analysis failed");
            }
            else
            {
                try
                {
                    detail.setIssuesJson(objectMapper.writeValueAsString(new AllIssues(result.getSecurityIssues(),
                        result.getPerformanceIssues(), result.getCodeStyleIssues())));
                }
                catch(JsonProcessingException e)
                {
                    log.warn("Failed to serialize issues for file: {}", result.getFilePath(), e);
                }
            }
            detail.setSummary(result.getSummary());
            detail.setDiffContent(result.getDiffContent());
            fileDetailMapper.insert(detail);
        }
        log.info("Review record saved: id={}, project={}, sha={}", record.getId(), record.getProjectId(),
            record.getCommitSha());
        return record;
    }

    @Override
    public Page<ReviewRecord> findByProject(String projectId, int page, int size)
    {
        Page<ReviewRecord> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<ReviewRecord> wrapper = new LambdaQueryWrapper<ReviewRecord>()
            .eq(ReviewRecord::getProjectId, projectId).orderByDesc(ReviewRecord::getCreatedAt);
        return reviewRecordMapper.selectPage(mpPage, wrapper);
    }

    @Override
    public List<ReviewRecord> findByProjectAndDateRange(String projectId, LocalDate from, LocalDate to)
    {
        return reviewRecordMapper.findByProjectIdAndDateRange(projectId, from.atStartOfDay(), to.atTime(LocalTime.MAX));
    }

    @Override
    public ReviewStats getStats(String projectId, LocalDate from, LocalDate to)
    {
        List<ReviewRecord> records = findByProjectAndDateRange(projectId, from, to);
        long blockingCount = reviewRecordMapper.countBlockingReviews(projectId, from.atStartOfDay(),
            to.atTime(LocalTime.MAX));
        int total = records.size();
        long totalDuration = records.stream().mapToLong(ReviewRecord::getDurationMs).sum();
        int totalCritical = records.stream().mapToInt(ReviewRecord::getCriticalCount).sum();
        int totalWarning = records.stream().mapToInt(ReviewRecord::getWarningCount).sum();
        int totalSuggestion = records.stream().mapToInt(ReviewRecord::getSuggestionCount).sum();
        return new ReviewStats(projectId, from, to, total, (int) blockingCount, totalCritical, totalWarning,
            totalSuggestion, total > 0 ? totalDuration / total : 0);
    }

    @Override
    public ReviewRecord findById(Long id)
    {
        ReviewRecord record = reviewRecordMapper.selectById(id);
        if(record != null)
        {
            List<ReviewFileDetail> details = fileDetailMapper
                .selectList(new LambdaQueryWrapper<ReviewFileDetail>().eq(ReviewFileDetail::getReviewId, id));
            record.setFileDetails(details);
        }
        return record;
    }

    @Override
    public List<ReviewRecord> getTrend(String projectId, int days)
    {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return reviewRecordMapper.findByProjectIdSince(projectId, since);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class AllIssues
    {
        private List<ReviewIssue> security;
        private List<ReviewIssue> performance;
        private List<ReviewIssue> codeStyle;
    }
}
