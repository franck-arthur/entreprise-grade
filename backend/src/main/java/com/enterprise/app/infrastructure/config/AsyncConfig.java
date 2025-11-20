package com.enterprise.app.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing and thread pool management.
 * Used for batch import operations and other multithreaded tasks.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool executor for batch import operations.
     * Configured with:
     * - Core pool size: 5 threads (minimum threads always alive)
     * - Max pool size: 20 threads (maximum threads under load)
     * - Queue capacity: 100 (number of tasks that can be queued)
     * - Thread name prefix: "batch-import-" for easy identification in logs
     * - Wait for tasks to complete on shutdown
     */
    @Bean(name = "batchImportExecutor")
    public Executor batchImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("batch-import-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool executor for CSV line processing.
     * Configured with higher parallelism for processing individual CSV lines:
     * - Core pool size: 10 threads
     * - Max pool size: 50 threads (high parallelism for I/O-bound operations)
     * - Queue capacity: 500 (large queue for many CSV lines)
     * - Thread name prefix: "csv-processor-"
     */
    @Bean(name = "csvProcessorExecutor")
    public Executor csvProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("csv-processor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
