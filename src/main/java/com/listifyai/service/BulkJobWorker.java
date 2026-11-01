package com.listifyai.service;

import com.listifyai.entity.BulkJobItem;
import com.listifyai.repository.BulkJobItemRepository;
import com.listifyai.repository.BulkJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
@SuppressWarnings({"null", "unchecked", "unused"})
public class BulkJobWorker {

    private static final Logger log = LoggerFactory.getLogger(BulkJobWorker.class);

    private final BulkJobItemRepository bulkJobItemRepository;
    private final BulkJobRepository bulkJobRepository;
    private final BulkJobProcessor bulkJobProcessor;
    private final BulkJobCompletionService bulkJobCompletionService;
    private final String workerId = UUID.randomUUID().toString();
    private final Executor bulkJobExecutor;

    public BulkJobWorker(BulkJobItemRepository bulkJobItemRepository,
                         BulkJobRepository bulkJobRepository,
                         BulkJobProcessor bulkJobProcessor,
                         BulkJobCompletionService bulkJobCompletionService,
                         @Qualifier("bulkJobExecutor") Executor bulkJobExecutor) {
        this.bulkJobItemRepository = bulkJobItemRepository;
        this.bulkJobRepository = bulkJobRepository;
        this.bulkJobProcessor = bulkJobProcessor;
        this.bulkJobCompletionService = bulkJobCompletionService;
        this.bulkJobExecutor = bulkJobExecutor;
    }

    @Scheduled(fixedDelay = 2000)
    public void pollAndProcess() {
        List<BulkJobItem> items = bulkJobItemRepository.claimNextItems(workerId, 10);
        if (!items.isEmpty()) {
            for (BulkJobItem item : items) {
                log.info("Item {} claimed by worker {}", item.getId(), workerId);
                bulkJobExecutor.execute(() -> {
                    try {
                        bulkJobProcessor.processSingleItem(item);
                    } catch (Exception e) {
                        log.error("Unhandled error processing item {}", item.getId(), e);
                    } finally {
                        bulkJobCompletionService.checkAndCompleteJob(item.getJob().getId());
                    }
                });
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void recoverStuckItems() {
        int recovered = bulkJobItemRepository.recoverStuckItems();
        if (recovered > 0) {
            log.info("Recovered {} stuck bulk job items", recovered);
        }
    }

}
