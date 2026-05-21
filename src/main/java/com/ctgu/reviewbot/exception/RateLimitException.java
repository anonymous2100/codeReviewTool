package com.ctgu.reviewbot.exception;

/**
 * @author lh2
 * @version 1.0
 * @description:
 * @date 2026-05-21 10:52
 */
public class RateLimitException extends RuntimeException
{
    public RateLimitException(String message)
    {
        super(message);
    }
}