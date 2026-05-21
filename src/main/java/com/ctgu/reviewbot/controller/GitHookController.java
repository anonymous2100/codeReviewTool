package com.ctgu.reviewbot.controller;

import com.ctgu.reviewbot.model.ApiResponse;
import com.ctgu.reviewbot.service.EventDispatcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Git 原生 Hook 接收端点（post-receive），适用于自建 Git 仓库
 */
@Slf4j
@RestController
@RequestMapping("/api/hook")
public class GitHookController
{
    private final EventDispatcherService dispatcherService;

    public GitHookController(EventDispatcherService dispatcherService)
    {
        this.dispatcherService = dispatcherService;
    }

    @PostMapping("/post-receive")
    public ResponseEntity<ApiResponse<Void>> handlePostReceive(@RequestBody Map<String, String> payload)
    {
        String oldRev = payload.get("oldrev");
        String newRev = payload.get("newrev");
        String ref = payload.get("refname");
        if(ref == null)
        {
            ref = payload.get("ref");
        }
        log.info("Received post-receive hook: oldRev={}, newRev={}, ref={}", oldRev, newRev, ref);
        if(newRev == null || newRev.isBlank() || "0000000000000000000000000000000000000000".equals(newRev))
        {
            log.info("Branch deleted, skipping review");
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        String projectId = payload.getOrDefault("project_id", "unknown");
        String branch = ref != null ? ref.replace("refs/heads/", "") : "unknown";
        dispatcherService.handlePushRaw(projectId, newRev, branch);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
