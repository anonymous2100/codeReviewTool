package com.ctgu.reviewbot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ctgu.reviewbot.model.ApiResponse;
import com.ctgu.reviewbot.model.ReviewRecord;
import com.ctgu.reviewbot.model.enums.ErrorCode;
import com.ctgu.reviewbot.service.ReviewRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 审查记录查询接口：列表、统计、趋势
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewQueryController
{
    private final ReviewRecordService reviewRecordService;

    public ReviewQueryController(ReviewRecordService reviewRecordService)
    {
        this.reviewRecordService = reviewRecordService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<IPage<ReviewRecord>>> listReviews(@RequestParam String projectId,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size)
    {
        IPage<ReviewRecord> records = reviewRecordService.findByProject(projectId, page, size);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ReviewRecordService.ReviewStats>> getStats(@RequestParam String projectId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
    {
        ReviewRecordService.ReviewStats stats = reviewRecordService.getStats(projectId, from, to);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewRecord>> getDetail(@PathVariable Long id)
    {
        ReviewRecord record = reviewRecordService.findById(id);
        if(record == null)
        {
            return ResponseEntity.status(404).body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), "Review not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<ReviewRecord>>> getTrend(@RequestParam String projectId,
        @RequestParam(defaultValue = "30") int days)
    {
        List<ReviewRecord> records = reviewRecordService.getTrend(projectId, days);
        return ResponseEntity.ok(ApiResponse.success(records));
    }
}
