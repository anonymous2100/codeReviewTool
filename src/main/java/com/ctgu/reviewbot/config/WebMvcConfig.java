package com.ctgu.reviewbot.config;

import com.ctgu.reviewbot.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 拦截器注册：API 限流（Webhook 鉴权由 WebhookSignatureFilter 处理）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer
{
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor)
    {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
    }
}
