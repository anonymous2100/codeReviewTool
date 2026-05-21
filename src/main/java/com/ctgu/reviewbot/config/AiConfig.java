package com.ctgu.reviewbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AI 提供商通用配置，对应 application.yml 中 ai.* 前缀。
 * <p>
 * 支持 DeepSeek / Zhipu / Kimi / Doubao 等 OpenAI 兼容 API。
 * </p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AiConfig
{
    /**
     * AI 提供商名称（deepseek / zhipu / kimi / doubao）
     */
    private String provider = "deepseek";
    /**
     * API 请求地址
     */
    private String baseUrl = "https://api.deepseek.com/v1";
    /**
     * API 密钥
     */
    private String apiKey;
    /**
     * 模型名称
     */
    private String model = "deepseek-v4-flash";
    /**
     * 生成最大 token 数
     */
    private int maxTokens = 2048;
    /**
     * 采样温度，越低越确定（0.0 ~ 1.0）
     */
    private double temperature = 0.15;
    /**
     * 核采样阈值
     */
    private double topP = 0.95;
    /**
     * 请求超时时间
     */
    private Duration timeout = Duration.ofSeconds(60);
}
