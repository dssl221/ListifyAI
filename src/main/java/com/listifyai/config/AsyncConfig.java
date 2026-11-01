package com.listifyai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Value("${async.thread.pool.size:5}")
    private int corePoolSize;

    @Value("${async.thread.pool.max:10}")
    private int maxPoolSize;

    @Value("${async.thread.pool.queue-capacity:500}")
    private int queueCapacity;

    @Value("${claude.http.thread.pool.size:10}")
    private int claudeHttpPoolSize;

    @Value("${claude.http.thread.pool.max:20}")
    private int claudeHttpPoolMax;

    @Bean(name = "bulkJobExecutor")
    public Executor bulkJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("bulk-job-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "claudeHttpExecutor")
    public Executor claudeHttpExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(claudeHttpPoolSize);
        executor.setMaxPoolSize(claudeHttpPoolMax);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("claude-http-");
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.initialize();
        return executor;
    }

    @Bean(name = "monitoringAiFixExecutor")
    public Executor monitoringAiFixExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("monitoring-ai-fix-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "emailOutboxExecutor")
    public Executor emailOutboxExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); executor.setMaxPoolSize(4); executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-outbox-"); executor.setWaitForTasksToCompleteOnShutdown(true); executor.setAwaitTerminationSeconds(30); executor.initialize(); return executor;
    }

    @Bean(name = "monitoringWorkerExecutor")
    public ThreadPoolTaskExecutor monitoringWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mon-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(name = "monitoringWorkerPollScheduler")
    public ThreadPoolTaskScheduler monitoringWorkerPollScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("monitoring-worker-poll-");
        scheduler.initialize();
        return scheduler;
    }
}
