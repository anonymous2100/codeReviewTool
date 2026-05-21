package com.ctgu.reviewbot.model.enums;

public enum ErrorCode
{
    INVALID_PARAM(1001, "参数错误"),
    AUTH_FAILED(1002, "认证失败"),
    GITLAB_API_ERROR(2001, "GitLab API 错误"),
    AI_SERVICE_ERROR(2002, "AI 服务异常"),
    REVIEW_TIMEOUT(2003, "审查超时"),
    INTERNAL_ERROR(3001, "内部错误"),
    REQUEST_BODY_ERROR(3002, "请求体错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message)
    {
        this.code = code;
        this.message = message;
    }

    public int getCode()
    {
        return code;
    }

    public String getMessage()
    {
        return message;
    }
}
