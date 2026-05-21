package com.ctgu.reviewbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 审查流程配置项，对应 application.yml 中 review.* 前缀。
 * <p>
 * 包含文件过滤、并发控制、超时、重试等参数。
 * </p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "review")
public class ReviewProperties
{
    /**
     * 单次审查最大文件数
     */
    private int maxFilesPerMr = 20;
    /**
     * 单个文件 diff 最大行数，超过则跳过审查
     */
    private int maxDiffLinesPerFile = 500;
    /**
     * AI 审查最大并发数
     */
    private int maxConcurrency = 5;
    /**
     * diff 截断字符数上限
     */
    private int maxDiffChars = 8000;
    /**
     * 单次审查超时时间（如 90s）
     */
    private String reviewTimeout = "90s";
    /**
     * 跳过审查的文件 glob 模式
     */
    private List<String> skipPatterns = List.of("**/*Test.java", "**/generated/**", "**/target/**");
    /**
     * 触发审查的目标分支列表，支持 glob 通配
     */
    private List<String> targetBranches = List.of("main", "master", "develop", "dev", "release/*");
    /**
     * 重试配置
     */
    private final Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry
    {
        /**
         * 最大重试次数
         */
        private int maxAttempts = 4;
        /**
         * 初始延迟（如 1000ms）
         */
        private Duration initialDelay = Duration.ofMillis(1000);
        /**
         * 退避倍数
         */
        private double multiplier = 2.0;
        /**
         * 最大延迟（如 10s）
         */
        private Duration maxDelay = Duration.ofSeconds(10);
    }
}
