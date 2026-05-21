package com.ctgu.reviewbot.model;

/**
 * 问题严重等级：NONE（无）< SUGGESTION（建议）< WARNING（警告）< CRITICAL（严重）
 */
public enum Severity
{
    NONE, SUGGESTION, WARNING, CRITICAL;

    /**
     * CRITICAL 等级视为阻塞性
     */
    public boolean isBlocking()
    {
        return this == CRITICAL;
    }
}
