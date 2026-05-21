package com.ctgu.reviewbot.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate Bean 配置，用于 GitLab API 调用
 */
@Configuration
public class RestTemplateConfig
{
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder)
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        return builder.requestFactory(() -> factory).build();
    }
}
