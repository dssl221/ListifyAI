package com.listifyai.service;

import com.listifyai.entity.BulkJob;
import com.listifyai.entity.BulkJobItem;
import com.listifyai.repository.BulkJobItemRepository;
import com.listifyai.repository.BulkJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BulkJobCompletionService {

    private static final Logger log = LoggerFactory.getLogger(BulkJobCompletionService.class);

    private final BulkJobRepository bulkJobRepository;
    private final BulkJobItemRepository bulkJobItemRepository;
    private final BulkJobProcessor bulkJobProcessor;

    public BulkJobCompletionService(BulkJobRepository bulkJobRepository,
                                    BulkJobItemRepository bulkJobItemRepository,
                                    BulkJobProcessor bulkJobProcessor) {
        this.bulkJobRepository = bulkJobRepository;
        this.bulkJobItemRepository = bulkJobItemRepository;
        this.bulkJobProcessor = bulkJobProcessor;
    }

    @Transactional
    public void checkAndCompleteJob(UUID jobId) {
        try {
            log.debug("Attempting to lock and check completion for job: {}", jobId);
            BulkJob job = bulkJobRepository.findJobForUpdate(jobId).orElse(null);
            if (job == null) {
                log.warn("Job {} not found during completion check", jobId);
                return;
            }

            if (Set.of("COMPLETED", "FAILED", "CANCELLED").contains(job.getStatus())) {
                log.debug("Job {} is already in terminal state: {}", jobId, job.getStatus());
                return;
            }

            List<BulkJobItem> allItems = bulkJobItemRepository.findByJobId(jobId);
            boolean allTerminal = allItems.stream().allMatch(i -> 
                Set.of("COMPLETED", "FAILED", "DEAD_LETTER", "CANCELLED").contains(i.getStatus())
            );

            int completed = (int) allItems.stream().filter(i -> "COMPLETED".equals(i.getStatus())).count();
            int failed = (int) allItems.stream().filter(i -> Set.of("FAILED", "DEAD_LETTER").contains(i.getStatus())).count();

            log.debug("Job {} counts recalculated. Completed: {}, Failed: {}", jobId, completed, failed);
            
            job.setCompletedItems(completed);
            job.setFailedItems(failed);

            if (allTerminal) {
                if (failed == allItems.size()) {
                    job.setStatus("FAILED");
                    log.info("Job {} completed with FAILED status (all items failed)", jobId);
                } else {
                    job.setStatus("COMPLETED");
                    log.info("Job {} completed successfully with COMPLETED status", jobId);
                }
                job.setCompletedAt(OffsetDateTime.now());

                try {
                    String csvUrl = bulkJobProcessor.buildResultCsvDataUrl(allItems);
                    job.setResultCsvUrl(csvUrl);
                } catch (Exception e) {
                    log.error("Failed to generate CSV for job {}", jobId, e);
                    job.setErrorLog("Failed to generate CSV: " + e.getMessage());
                }
            }
            
            bulkJobRepository.save(job);
        } catch (Exception e) {
            log.error("Lock acquisition or transaction failure during completion check for job {}", jobId, e);
            throw e;
        }
    }
}
