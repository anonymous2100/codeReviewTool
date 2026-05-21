package com.ctgu.reviewbot.exception;

/**
 * @author lh2
 * @version 1.0
 * @description:
 * @date 2026-05-21 10:53
 */
public class ReviewFailedException extends RuntimeException
{
    public ReviewFailedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}