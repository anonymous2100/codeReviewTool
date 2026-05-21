package com.ctgu.reviewbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应包装，所有 REST 接口均返回此结构。
 *
 * @param <T>
 *            data 载荷类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T>
{
    /**
     * 状态码，0 表示成功
     */
    private int code;
    /**
     * 响应消息
     */
    private String message;
    /**
     * 响应数据载荷
     */
    private T data;
    /**
     * 服务器时间戳
     */
    private long timestamp;

    /**
     * 构造成功响应
     */
    public static <T> ApiResponse<T> success(T data)
    {
        return new ApiResponse<>(0, "success", data, System.currentTimeMillis());
    }

    /**
     * 构造错误响应
     */
    public static <T> ApiResponse<T> error(int code, String message)
    {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis());
    }
}
