package com.ctgu.reviewbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GitLab API 配置，对应 application.yml 中 gitlab.* 前缀
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gitlab")
public class GitLabConfig
{
    /**
     * GitLab API 基础地址
     */
    private String baseUrl = "https://gitlab.com/api/v4";
    /**
     * GitLab 个人访问令牌（PRIVATE-TOKEN）
     */
    private String token;
    /**
     * 连接超时（如 5s）
     */
    private String connectTimeout = "5s";
    /**
     * 读取超时（如 30s）
     */
    private String readTimeout = "30s";
}
