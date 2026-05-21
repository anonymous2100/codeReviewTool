package com.ctgu.reviewbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Webhook 安全配置，对应 application.yml 中 webhook.* 前缀
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "webhook")
public class WebhookProperties
{
    private String secret;
}
