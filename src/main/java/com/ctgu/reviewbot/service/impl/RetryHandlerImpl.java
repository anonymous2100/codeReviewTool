package com.ctgu.reviewbot.service.impl;

import com.ctgu.reviewbot.service.RetryHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
public class RetryHandlerImpl implements RetryHandler
{
    @Override
    public <T> T executeWithRetry(Supplier<T> operation, String operationId) throws Exception
    {
        log.info("Executing operation: {}", operationId);
        return operation.get();
    }
}
