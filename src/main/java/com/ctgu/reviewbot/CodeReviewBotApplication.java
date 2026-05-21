package com.ctgu.reviewbot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Code Review Bot — Spring Boot 入口。
 * <p>
 * 自动审查 GitLab 合并请求 / 推送代码 提交的 Java 代码变更，通过 AI 生成审查报告并回写到 GitLab。
 * </p>
 */
@SpringBootApplication
@EnableRetry
@EnableAsync
@ConfigurationPropertiesScan
@MapperScan("com.ctgu.reviewbot.mapper")
public class CodeReviewBotApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(CodeReviewBotApplication.class, args);
    }
}
