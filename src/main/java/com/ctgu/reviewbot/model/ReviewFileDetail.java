package com.ctgu.reviewbot.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 审查文件详情实体 — 对应 review_file_details 表。
 * <p>
 * 记录每个文件在本次审查中的结果，包括问题列表 JSON 和摘要。
 * </p>
 */
@Data
@TableName("review_file_details")
public class ReviewFileDetail
{
    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联 review_records 表 ID
     */
    @TableField("review_id")
    private Long reviewId;

    /**
     * 文件路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 新增行数
     */
    @TableField("added_lines")
    private int addedLines;

    /**
     * 删除行数
     */
    @TableField("removed_lines")
    private int removedLines;

    /**
     * 问题列表 JSON
     */
    @TableField("issues_json")
    private String issuesJson;

    /**
     * 审查摘要
     */
    @TableField("summary")
    private String summary;

    /**
     * 是否跳过审查
     */
    @TableField("is_skipped")
    private boolean isSkipped;

    /**
     * 跳过原因
     */
    @TableField("skip_reason")
    private String skipReason;

    /**
     * 文件 diff 内容
     */
    @TableField("diff_content")
    private String diffContent;
}
