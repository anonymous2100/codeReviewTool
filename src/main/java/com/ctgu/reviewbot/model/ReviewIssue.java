package com.ctgu.reviewbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 审查发现的单个问题。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIssue
{
    /**
     * 代码行号
     */
    private int lineNumber;
    /**
     * 严重等级
     */
    private Severity severity;
    /**
     * 分类 (security / performance / code_style)
     */
    private String category;
    /**
     * 问题描述
     */
    private String description;
    /**
     * 修复建议
     */
    private String suggestion;
}
