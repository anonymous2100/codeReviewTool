package com.ctgu.reviewbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件差异模型，表示一个文件的 Git Diff 及其元信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDiff
{
    /**
     * 文件路径
     */
    private String path;
    /**
     * 完整 diff 内容
     */
    private String diff;
    /**
     * 是否新增文件
     */
    private boolean isNew;
    /**
     * 是否删除文件
     */
    private boolean isDeleted;
    /**
     * 新增行数
     */
    private int addedLines;
    /**
     * 删除行数
     */
    private int removedLines;

    public boolean isJavaFile()
    {
        return path.endsWith(".java");
    }

    public int lineCount()
    {
        return addedLines + removedLines;
    }

    public static FileDiff fromGitLabDiff(GitLabDiffDto dto)
    {
        String diffContent = dto.getDiff() != null ? dto.getDiff() : "";
        return new FileDiff(dto.getNewPath() != null ? dto.getNewPath() : dto.getOldPath(), diffContent, dto.isNewFile(),
            dto.isDeletedFile(), countLines(diffContent, '+'), countLines(diffContent, '-'));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class GitLabDiffDto
    {
        /**
         * 原文件路径
         */
        @JsonProperty("old_path")
        private String oldPath;

        /**
         * 新文件路径
         */
        @JsonProperty("new_path")
        private String newPath;

        /**
         * diff 内容
         */
        @JsonProperty("diff")
        private String diff;

        /**
         * 是否新增文件
         */
        @JsonProperty("new_file")
        private boolean newFile;

        /**
         * 是否重命名文件
         */
        @JsonProperty("renamed_file")
        private boolean renamedFile;

        /**
         * 是否删除文件
         */
        @JsonProperty("deleted_file")
        private boolean deletedFile;
    }

    private static int countLines(String diff, char prefix)
    {
        if(diff == null || diff.isEmpty())
        {
            return 0;
        }
        int count = 0;
        for(String line : diff.split("\n"))
        {
            if(line.startsWith(String.valueOf(prefix)) && !line.startsWith(prefix + "+") && !line.startsWith(prefix + "-"))
            {
                count++;
            }
        }
        return count;
    }
}
