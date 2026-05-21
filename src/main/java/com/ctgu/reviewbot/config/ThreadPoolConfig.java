package com.ctgu.reviewbot.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 异步审查线程池配置
 */
@Configuration
public class ThreadPoolConfig
{
    @Bean("reviewExecutor")
    public ExecutorService reviewExecutor(ExecutorProperties props)
    {
        return new ThreadPoolExecutor(props.getCorePoolSize(), props.getMaxPoolSize(), 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(props.getQueueCapacity()), new ThreadFactoryBuilder().setNameFormat("review-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "executor.review")
    public static class ExecutorProperties
    {
        private int corePoolSize = 2;
        private int maxPoolSize = 4;
        private int queueCapacity = 100;
    }
}
