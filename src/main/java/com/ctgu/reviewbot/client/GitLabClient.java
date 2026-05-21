package com.ctgu.reviewbot.client;

import com.ctgu.reviewbot.config.GitLabConfig;
import com.ctgu.reviewbot.model.FileDiff.GitLabDiffDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GitLab API 客户端：获取 合并请求/提交记录 差异、创建评论和笔记
 */
@Slf4j
@Component
public class GitLabClient
{
    private final RestTemplate restTemplate;
    private final GitLabConfig config;

    /**
     * @param restTemplate
     *            Spring RestTemplate
     * @param config
     *            GitLab 配置
     */
    public GitLabClient(RestTemplate restTemplate, GitLabConfig config)
    {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    /**
     * 获取 合并请求 的文件差异列表（支持分页）
     *
     * @param projectId
     *            GitLab 项目 ID
     * @param mergeRequestIid
     *            合并请求 IID
     * @return GitLabDiffDto 列表
     */
    public List<GitLabDiffDto> getMrDiffs(String projectId, Long mergeRequestIid)
    {
        return fetchPaginatedList(GitLabApiPaths.MR_DIFFS, new ParameterizedTypeReference<>()
        {
        }, encode(projectId), mergeRequestIid);
    }

    /**
     * 获取指定 提交记录 的文件差异列表
     *
     * @param projectId
     *            GitLab 项目 ID
     * @param commitSha
     *            提交记录 SHA
     * @return GitLabDiffDto 列表
     */
    public List<GitLabDiffDto> getCommitDiff(String projectId, String commitSha)
    {
        String url = config.getBaseUrl() + GitLabApiPaths.COMMIT_DIFF;
        log.info("Calling GitLab API: projectId={}, sha={}, url={}", projectId, commitSha,
            config.getBaseUrl() + GitLabApiPaths.COMMIT_DIFF);
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try
        {
            ResponseEntity<List<GitLabDiffDto>> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<GitLabDiffDto>>()
                {
                }, encode(projectId), commitSha);
            List<GitLabDiffDto> items = response.getBody();
            return items != null ? items : List.of();
        }
        catch(Exception e)
        {
            log.error("GitLab API call failed: projectId={}, sha={}, url={}", projectId, commitSha,
                config.getBaseUrl() + GitLabApiPaths.COMMIT_DIFF, e);
            throw e;
        }
    }

    /**
     * 获取当前用户有权限访问的项目列表
     *
     * @return 项目列表 DTO
     */
    public List<ProjectDto> getProjects()
    {
        String url = config.getBaseUrl() + GitLabApiPaths.PROJECTS;
        log.info("Fetching GitLab projects");
        HttpHeaders headers = authHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<List<ProjectDto>> response =
            restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<ProjectDto>>()
            {
            });
        List<ProjectDto> items = response.getBody();
        return items != null ? items : List.of();
    }

    /**
     * 获取合并请求详情
     *
     * @param projectId
     * @param mergeRequestIid
     * @return
     */
    public MrDetailDto getMrDetail(String projectId, Long mergeRequestIid)
    {
        String url = config.getBaseUrl() + GitLabApiPaths.MR_DETAIL;
        return restTemplate.getForObject(url, MrDetailDto.class, encode(projectId), mergeRequestIid);
    }

    /**
     * 在 合并请求 上创建一条笔记（评论）
     *
     * @param projectId
     *            GitLab 项目 ID
     * @param mergeRequestIid
     *            合并请求 IID
     * @param body
     *            评论正文
     */
    public void createMrNote(String projectId, Long mergeRequestIid, String body)
    {
        String url = config.getBaseUrl() + GitLabApiPaths.MR_NOTES;
        HttpHeaders headers = authHeaders();
        Map<String, String> payload = Map.of("body", body);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);
        log.info("Posting review comment to 合并请求: project={}, mergeRequestIid={}", projectId, mergeRequestIid);
        restTemplate.postForObject(url, entity, Void.class, encode(projectId), mergeRequestIid);
    }

    /**
     * 在指定 提交记录 上创建一条评论
     *
     * @param projectId
     *            GitLab 项目 ID
     * @param sha
     *            提交记录 SHA
     * @param body
     *            评论正文
     */
    public void createCommitComment(String projectId, String sha, String body)
    {
        String url = config.getBaseUrl() + GitLabApiPaths.COMMIT_COMMENTS;
        HttpHeaders headers = authHeaders();

        Map<String, String> payload = Map.of("note", body);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        log.info("Posting review comment to commit: project={}, sha={}", projectId, sha);
        restTemplate.postForObject(url, entity, Void.class, encode(projectId), sha);
    }

    /**
     * URL 路径编码
     */
    private String encode(String value)
    {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    /**
     * 构造携带 PRIVATE-TOKEN 的认证请求头
     */
    private HttpHeaders authHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("PRIVATE-TOKEN", config.getToken());
        return headers;
    }

    /**
     * 通用分页获取方法：逐页请求直到无下一页
     *
     * @param pathTemplate
     *            路径模板（不含 baseUrl）
     * @param typeRef
     *            响应的泛型类型引用
     * @param uriVars
     *            URL 模板变量
     * @param <T>
     *            列表元素类型
     * @return 合并后的全量列表
     */
    private <T> List<T> fetchPaginatedList(String pathTemplate, ParameterizedTypeReference<List<T>> typeRef, Object... uriVars)
    {
        List<T> allItems = new ArrayList<>();
        int page = 1;
        while(true)
        {
            String url = config.getBaseUrl() + pathTemplate + "&page=" + page;
            HttpHeaders headers = authHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<List<T>> response = restTemplate.exchange(url, HttpMethod.GET, entity, typeRef, uriVars);
            List<T> items = response.getBody();
            if(items == null || items.isEmpty())
            {
                break;
            }
            allItems.addAll(items);
            String nextPage = response.getHeaders().getFirst("X-Next-Page");
            if(nextPage == null || nextPage.isEmpty())
            {
                break;
            }
            page++;
        }
        return allItems;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectDto
    {
        @JsonProperty("id")
        private Long id;
        @JsonProperty("name")
        private String name;
        @JsonProperty("name_with_namespace")
        private String nameWithNamespace;
        @JsonProperty("path_with_namespace")
        private String pathWithNamespace;
        @JsonProperty("description")
        private String description;
        @JsonProperty("web_url")
        private String webUrl;
        @JsonProperty("default_branch")
        private String defaultBranch;
        @JsonProperty("visibility")
        private String visibility;
        @JsonProperty("last_activity_at")
        private String lastActivityAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MrDetailDto
    {
        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("source_branch")
        private String sourceBranch;

        @JsonProperty("target_branch")
        private String targetBranch;

        @JsonProperty("state")
        private String state;
    }
}
