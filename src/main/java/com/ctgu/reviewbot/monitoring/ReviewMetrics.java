package com.ctgu.reviewbot.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Micrometer 监控指标收集：审查次数、API 耗时/失败、跳过文件、并发数
 */
@Slf4j
@Component
public class ReviewMetrics
{
    /**
     * 审查完成总数计数器
     */
    private final Counter reviewsTotal;
    /**
     * AI API 调用耗时计时器
     */
    private final Timer apiCallDuration;
    /**
     * AI API 调用失败次数计数器
     */
    private final Counter apiCallFailures;
    /**
     * 跳过文件数计数器
     */
    private final Counter skippedFiles;
    /**
     * 当前进行中的审查数量
     */
    private final AtomicInteger inProgressCount;

    public ReviewMetrics(MeterRegistry meterRegistry)
    {
        this.reviewsTotal = Counter.builder("reviewbot.reviews.total").description("Total number of reviews")
            .tag("app", "code-review-bot").register(meterRegistry);
        this.apiCallDuration = Timer.builder("reviewbot.api.call.duration").description("AI API call duration")
            .register(meterRegistry);
        this.apiCallFailures = Counter.builder("reviewbot.api.call.failures").description("AI API call failures")
            .register(meterRegistry);
        this.skippedFiles = Counter.builder("reviewbot.files.skipped").description("Files skipped during review")
            .register(meterRegistry);
        this.inProgressCount = new AtomicInteger(0);
        meterRegistry.gauge("reviewbot.reviews.in_progress", inProgressCount);
    }

    public void recordReviewCompleted(String status, int fileCount, long durationMs)
    {
        reviewsTotal.increment();
        log.info("Review completed: status={}, files={}, durationMs={}", status, fileCount, durationMs);
    }

    public void recordApiCall(String filePath, long durationMs, boolean success)
    {
        apiCallDuration.record(durationMs, TimeUnit.MILLISECONDS);
        if(!success)
        {
            apiCallFailures.increment();
        }
    }

    public void recordSkippedFile(String filePath, String reason)
    {
        skippedFiles.increment();
        log.info("File skipped: path={}, reason={}", filePath, reason);
    }

    public void incrementInProgress()
    {
        inProgressCount.incrementAndGet();
    }

    public void decrementInProgress()
    {
        inProgressCount.decrementAndGet();
    }
}
