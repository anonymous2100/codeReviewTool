package com.ctgu.reviewbot.controller;

import com.ctgu.reviewbot.exception.ReviewFailedException;
import com.ctgu.reviewbot.model.ApiResponse;
import com.ctgu.reviewbot.model.enums.ErrorCode;
import com.ctgu.reviewbot.service.EventDispatcherService.AiServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

/**
 * 全局异常处理器：统一返回 ApiResponse 错误结构
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler(WebhookAuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(WebhookAuthException e)
    {
        log.warn("Webhook auth failed: {}", e.getMessage());
        return ResponseEntity.status(403).body(ApiResponse.error(ErrorCode.AUTH_FAILED.getCode(), "Invalid webhook token"));
    }

    @ExceptionHandler(DiffFetchException.class)
    public ResponseEntity<ApiResponse<Void>> handleDiffFetch(DiffFetchException e)
    {
        log.error("Failed to fetch diff: project={}, mr={}", e.getProjectId(), e.getMrIid(), e);
        return ResponseEntity.status(502)
            .body(ApiResponse.error(ErrorCode.AI_SERVICE_ERROR.getCode(), "Failed to fetch diff from GitLab: " + e.getMessage()));
    }

    @ExceptionHandler(AiReviewTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleTimeout(AiReviewTimeoutException e)
    {
        log.error("AI review timed out for: {}", e.getFilePath());
        return ResponseEntity.status(504).body(ApiResponse.error(ErrorCode.REVIEW_TIMEOUT.getCode(), "AI review timed out"));
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiUnavailable(AiServiceUnavailableException e)
    {
        log.error("AI service unavailable", e);
        return ResponseEntity.status(502).body(ApiResponse.error(ErrorCode.AI_SERVICE_ERROR.getCode(), "AI API service unavailable"));
    }

    @ExceptionHandler(ReviewFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleReviewFailed(ReviewFailedException e)
    {
        log.error("Review failed", e);
        return ResponseEntity.status(500).body(ApiResponse.error(ErrorCode.AI_SERVICE_ERROR.getCode(), "Review failed: " + e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e)
    {
        if(e.getMostSpecificCause() instanceof java.io.EOFException)
        {
            log.warn("Client disconnected before request body was fully read (EOF), ignoring");
            return ResponseEntity.status(400).body(ApiResponse.error(ErrorCode.REQUEST_BODY_ERROR.getCode(), "Request body incomplete"));
        }
        log.warn("Failed to read request body: {}", e.getMessage());
        return ResponseEntity.status(400).body(ApiResponse.error(ErrorCode.REQUEST_BODY_ERROR.getCode(), "Invalid request body"));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ApiResponse<Void>> handleGitLabApiError(HttpClientErrorException e)
    {
        log.error("GitLab API returned error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
        String message = String.format("GitLab API error (%s): %s", e.getStatusCode(), e.getMessage());
        return ResponseEntity.status(e.getStatusCode()).body(ApiResponse.error(ErrorCode.GITLAB_API_ERROR.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e)
    {
        log.error("Unexpected error", e);
        return ResponseEntity.status(500).body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), "Internal error"));
    }

    public static class WebhookAuthException extends RuntimeException
    {
        public WebhookAuthException(String message)
        {
            super(message);
        }
    }

    public static class DiffFetchException extends RuntimeException
    {
        private final String projectId;
        private final Long mergeRequestIid;

        public DiffFetchException(String projectId, Long mergeRequestIid, String message, Throwable cause)
        {
            super(message, cause);
            this.projectId = projectId;
            this.mergeRequestIid = mergeRequestIid;
        }

        public String getProjectId()
        {
            return projectId;
        }

        public Long getMrIid()
        {
            return mergeRequestIid;
        }
    }

    public static class AiReviewTimeoutException extends RuntimeException
    {
        private final String filePath;

        public AiReviewTimeoutException(String filePath, String message)
        {
            super(message);
            this.filePath = filePath;
        }

        public String getFilePath()
        {
            return filePath;
        }
    }
}
