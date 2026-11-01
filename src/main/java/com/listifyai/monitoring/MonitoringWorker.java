package com.listifyai.monitoring;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class MonitoringWorker {

    private static final Logger log = LoggerFactory.getLogger(MonitoringWorker.class);

    private final MonitoringQueueService queueService;
    private final MonitoringJobProcessor processor;
    private final MonitoringProperties properties;
    private final ThreadPoolTaskExecutor monitoringWorkerExecutor;
    private volatile boolean shuttingDown = false;

    public MonitoringWorker(MonitoringQueueService queueService,
                            MonitoringJobProcessor processor,
                            MonitoringProperties properties,
                            @Qualifier("monitoringWorkerExecutor") ThreadPoolTaskExecutor monitoringWorkerExecutor) {
        this.queueService = queueService;
        this.processor = processor;
        this.properties = properties;
        this.monitoringWorkerExecutor = monitoringWorkerExecutor;
    }

    @Scheduled(fixedDelayString = "${monitoring.poll.interval:5000}")
    public void scheduledPoll() {
        poll();
    }

    public void poll() {
        if (shuttingDown || !properties.isEnabled()) {
            return;
        }
        for (int i = 0; i < properties.getWorkerBatchSize(); i++) {
            Optional<UUID> jobId = queueService.pollJobId();
            if (jobId.isEmpty() || shuttingDown) {
                return;
            }
            CompletableFuture.runAsync(() -> processJob(jobId.get()), monitoringWorkerExecutor);
        }
    }

    private void processJob(UUID jobId) {
        if (shuttingDown) {
            return;
        }
        try {
            processor.process(jobId);
        } catch (Exception ex) {
            log.error("Unexpected monitoring worker failure for job {}", jobId, ex);
        }
    }

    @PreDestroy
    public void drain() {
        shuttingDown = true;
        monitoringWorkerExecutor.shutdown();
        try {
            monitoringWorkerExecutor.getThreadPoolExecutor().awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
