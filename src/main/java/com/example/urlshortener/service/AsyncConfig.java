package com.example.urlshortener.service;

import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Bounded executor for analytics capture. Under a click storm the queue fills and
     * events are dropped with a log line — analytics is best-effort by design and must
     * never back-pressure the redirect path (brownfield-analytics.md, failure scenarios).
     */
    @Bean(name = "analyticsExecutor")
    public ThreadPoolTaskExecutor analyticsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("analytics-");
        executor.setRejectedExecutionHandler(
                (Runnable r, ThreadPoolExecutor e) -> log.warn("Analytics executor saturated; dropping click event"));
        return executor;
    }
}
