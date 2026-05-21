package com.ctgu.reviewbot.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 IP 的 API 限流拦截器：每分钟最多 60 次请求
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor
{
    private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES).maximumSize(10_000).build();

    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        String clientIp = getClientIp(request);
        AtomicInteger count = requestCounts.get(clientIp, k -> new AtomicInteger(0));
        if(count != null && count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE)
        {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(429);
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request)
    {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if(xForwardedFor != null && !xForwardedFor.isBlank())
        {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
