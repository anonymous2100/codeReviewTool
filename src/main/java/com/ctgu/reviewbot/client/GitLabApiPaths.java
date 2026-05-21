package com.ctgu.reviewbot.client;

/**
 * GitLab REST API 路径常量。
 * <p>
 * 所有路径均为相对路径，拼接 {@code baseUrl} 后使用， 路径中的 {id}/{sha}/{mrIid} 等占位符由 Spring RestTemplate 按位置替换。
 * </p>
 */
public final class GitLabApiPaths
{
    /**
     * 合并请求文件差异列表，分页
     */
    public static final String MR_DIFFS = "/projects/{id}/merge_requests/{mrIid}/diffs?per_page=100";

    /**
     * 提交记录文件差异列表
     */
    public static final String COMMIT_DIFF = "/projects/{id}/repository/commits/{sha}/diff?per_page=100";

    /**
     * 合并请求详情
     */
    public static final String MR_DETAIL = "/projects/{id}/merge_requests/{mrIid}";

    /**
     * 合并请求笔记（评论）
     */
    public static final String MR_NOTES = "/projects/{id}/merge_requests/{mrIid}/notes";

    /**
     * 提交记录评论
     */
    public static final String COMMIT_COMMENTS = "/projects/{id}/repository/commits/{sha}/comments";

    /**
     * 项目列表（当前用户有权限的项目）
     */
    public static final String PROJECTS = "/projects?membership=true&per_page=100&simple=true";

    private GitLabApiPaths()
    {
    }
}
