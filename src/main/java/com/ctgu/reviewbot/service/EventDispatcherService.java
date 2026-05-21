package com.ctgu.reviewbot.service;

import com.ctgu.reviewbot.model.WebhookEvent;

public interface EventDispatcherService
{
    void handlePush(WebhookEvent event);

    void handlePushRaw(String projectId, String commitSha, String branch);

    void handleMergeRequest(WebhookEvent event);

    boolean shouldTriggerReview(WebhookEvent event);

    public class AiServiceUnavailableException extends RuntimeException
    {
        public AiServiceUnavailableException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
