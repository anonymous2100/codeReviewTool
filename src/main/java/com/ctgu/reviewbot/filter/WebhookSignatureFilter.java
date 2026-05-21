package com.ctgu.reviewbot.filter;

import com.ctgu.reviewbot.config.WebhookProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * GitLab Signing Token 验签过滤器：HMAC-SHA256 签名验证，同时缓存请求体供下游读取
 */
@Slf4j
@Component
@Order(1)
public class WebhookSignatureFilter extends OncePerRequestFilter
{
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final WebhookProperties webhookProperties;

    public WebhookSignatureFilter(WebhookProperties webhookProperties)
    {
        this.webhookProperties = webhookProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        String path = request.getRequestURI();
        return !path.startsWith("/api/webhook/") && !path.startsWith("/api/hook/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException
    {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        log.info("Webhook request: method={}, uri={}, contentLength={}, contentType={}", wrappedRequest.getMethod(),
            wrappedRequest.getRequestURI(), wrappedRequest.getContentLengthLong(), wrappedRequest.getContentType());
        String secret = webhookProperties.getSecret();
        if(secret == null || secret.isBlank())
        {
            log.warn("Webhook secret not configured — accepting all requests");
            chain.doFilter(wrappedRequest, response);
            return;
        }
        String token = request.getHeader("X-Gitlab-Token");
        if(token == null)
        {
            token = request.getHeader("X-Hook-Secret");
        }
        if(token == null || token.isBlank())
        {
            log.warn("Webhook request missing token header");
            response.sendError(403, "Missing webhook token");
            return;
        }
        byte[] bodyBytes = getBodyBytes(wrappedRequest);
        String expectedSignature = computeHmac(secret, bodyBytes);
        if(!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
            token.getBytes(StandardCharsets.UTF_8)))
        {
            log.warn("Webhook HMAC signature mismatch");
            response.sendError(403, "Invalid webhook signature");
            return;
        }
        log.info("Webhook HMAC signature verified");
        chain.doFilter(wrappedRequest, response);
    }

    private String computeHmac(String secret, byte[] data)
    {
        try
        {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] signature = mac.doFinal(data);
            return Base64.getEncoder().encodeToString(signature);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }

    private byte[] getBodyBytes(ContentCachingRequestWrapper request) throws IOException
    {
        request.getInputStream().readAllBytes();
        return request.getContentAsByteArray();
    }
}
