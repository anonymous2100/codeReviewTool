package com.ctgu.reviewbot.controller;

import com.ctgu.reviewbot.model.ApiResponse;
import com.ctgu.reviewbot.model.WebhookEvent;
import com.ctgu.reviewbot.service.EventDispatcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GitLab Webhook 接收端点，分发 Push Hook 和 Merge Request Hook 事件
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
public class WebhookController
{
    private final EventDispatcherService dispatcherService;

    public WebhookController(EventDispatcherService dispatcherService)
    {
        this.dispatcherService = dispatcherService;
    }

    @PostMapping("/gitlab")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(@RequestHeader("X-Gitlab-Event") String eventType,
        @RequestHeader(value = "X-Gitlab-Token", required = false) String token, @RequestBody WebhookEvent event)
    {
        log.info("Received GitLab webhook: type={}, project={}", eventType, event.projectId());
        switch(eventType){
            case "Push Hook" ->
            {
                dispatcherService.handlePush(event);
                log.info("Push event processed: project={}, sha={}", event.projectId(), event.commitSha());
            }
            case "Merge Request Hook" ->
            {
                if(dispatcherService.shouldTriggerReview(event))
                {
                    dispatcherService.handleMergeRequest(event);
                    log.info("MR event processing started: project={}, mrIid={}", event.projectId(),
                        event.mergeRequestIid());
                }
                else
                {
                    log.info("MR event skipped (not open or target branch not matched): project={}, mrIid={}",
                        event.projectId(), event.mergeRequestIid());
                }
            }
            default -> log.info("Ignored event type: {}", eventType);
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
