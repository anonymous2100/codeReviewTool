package com.ctgu.reviewbot.service;

import com.ctgu.reviewbot.exception.RateLimitException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public interface RetryHandler
{
    @Retryable(retryFor = { RateLimitException.class, TimeoutException.class, SocketTimeoutException.class }, //
               maxAttemptsExpression = "#{@reviewProperties.getRetry().getMaxAttempts()}", //
               backoff = @Backoff(delayExpression = "#{@reviewProperties.getRetry().getInitialDelay().toMillis()}", //
                                  multiplierExpression = "#{@reviewProperties.getRetry().getMultiplier()}", //
                                  maxDelayExpression = "#{@reviewProperties.getRetry().getMaxDelay().toMillis()}"))
    <T> T executeWithRetry(Supplier<T> operation, String operationId) throws Exception;

}



