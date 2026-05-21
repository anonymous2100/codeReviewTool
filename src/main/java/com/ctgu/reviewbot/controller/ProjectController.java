package com.ctgu.reviewbot.controller;

import com.ctgu.reviewbot.client.GitLabClient;
import com.ctgu.reviewbot.model.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController
{
    private final GitLabClient gitLabClient;

    public ProjectController(GitLabClient gitLabClient)
    {
        this.gitLabClient = gitLabClient;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GitLabClient.ProjectDto>>> listProjects()
    {
        List<GitLabClient.ProjectDto> projects = gitLabClient.getProjects();
        return ResponseEntity.ok(ApiResponse.success(projects));
    }
}
