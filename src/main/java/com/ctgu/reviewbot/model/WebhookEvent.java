package com.ctgu.reviewbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * GitLab Webhook 推送事件载荷映射。
 * <p>
 * 同时兼容 Push Hook 和 Merge Request Hook 两种事件类型。
 * </p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookEvent
{
    /**
     * 事件类型（push / merge_request）
     */
    @JsonProperty("object_kind")
    private String objectKind;

    /**
     * 项目引用信息
     */
    @JsonProperty("project")
    private ProjectRef project;

    /**
     * MR 属性（Push 事件为 null）
     */
    @JsonProperty("object_attributes")
    private MrAttributes objectAttributes;

    /**
     * Push 事件的 commit SHA
     */
    @JsonProperty("checkout_sha")
    private String checkoutSha;

    public String projectId()
    {
        return project != null ? String.valueOf(project.getId()) : null;
    }

    public Long mergeRequestIid()
    {
        return objectAttributes != null ? objectAttributes.getIid() : null;
    }

    public String commitSha()
    {
        if(objectAttributes != null && objectAttributes.getLastCommit() != null)
        {
            return objectAttributes.getLastCommit().getId();
        }
        return checkoutSha;
    }

    public String sourceBranch()
    {
        return objectAttributes != null ? objectAttributes.getSourceBranch() : null;
    }

    public String targetBranch()
    {
        return objectAttributes != null ? objectAttributes.getTargetBranch() : null;
    }

    public String mrState()
    {
        return objectAttributes != null ? objectAttributes.getState() : null;
    }

    public boolean isOpenMergeRequest()
    {
        return "opened".equals(mrState());
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectRef
    {
        /**
         * 项目数字 ID
         */
        @JsonProperty("id")
        private Long id;

        /**
         * 项目路径（如 namespace/project）
         */
        @JsonProperty("path_with_namespace")
        private String pathWithNamespace;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MrAttributes
    {
        /**
         * MR 内部 IID
         */
        @JsonProperty("iid")
        private Long iid;

        /**
         * MR 状态（opened / merged / closed）
         */
        @JsonProperty("state")
        private String state;

        /**
         * 源分支
         */
        @JsonProperty("source_branch")
        private String sourceBranch;

        /**
         * 目标分支
         */
        @JsonProperty("target_branch")
        private String targetBranch;

        /**
         * 最后一次 commit
         */
        @JsonProperty("last_commit")
        private LastCommit lastCommit;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LastCommit
    {
        /**
         * Commit SHA
         */
        @JsonProperty("id")
        private String id;
    }
}
