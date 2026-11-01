package com.listifyai.monitoring;

import com.listifyai.dto.EtsyAuditDtos.CompactEtsyListing;
import com.listifyai.entity.MonitoredListing;
import com.listifyai.entity.MonitoringJob;
import com.listifyai.exception.EtsyUnavailableException;
import com.listifyai.monitoring.MonitoringEnums.MonitoringJobStatus;
import com.listifyai.repository.MonitoringJobRepository;
import com.listifyai.repository.MonitoredListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class MonitoringJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(MonitoringJobProcessor.class);

    private final MonitoringJobRepository jobRepository;
    private final ListingMonitoringService monitoringService;
    private final MonitoringQueueService queueService;
    private final MonitoringProperties properties;
    private final MonitoringPlanService planService;
    private final MonitoredListingRepository monitoredListingRepository;
    private final TransactionTemplate transactionTemplate;

    public MonitoringJobProcessor(MonitoringJobRepository jobRepository,
                                  ListingMonitoringService monitoringService,
                                  MonitoringQueueService queueService,
                                  MonitoringProperties properties,
                                  MonitoringPlanService planService,
                                  MonitoredListingRepository monitoredListingRepository,
                                  TransactionTemplate transactionTemplate) {
        this.jobRepository = jobRepository;
        this.monitoringService = monitoringService;
        this.queueService = queueService;
        this.properties = properties;
        this.planService = planService;
        this.monitoredListingRepository = monitoredListingRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public void process(UUID jobId) {
        String lockKey = "tagwing:monitoring:job:" + jobId;
        if (!queueService.acquireLock(lockKey, properties.getLockTtl())) {
            return;
        }

        try {
            Optional<MonitoredListing> listing = claim(jobId);
            if (listing.isEmpty()) {
                return;
            }

            try {
                CompactEtsyListing liveListing = monitoringService.fetchLiveListing(listing.get());
                monitoringService.persistMonitoringResult(listing.get().getId(), jobId, liveListing);
                complete(jobId);
            } catch (EtsyUnavailableException ex) {
                log.warn("Etsy unavailable for monitoring job {}; requeueing", jobId, ex);
                requeueForEtsyOutage(jobId, ex);
            } catch (Exception ex) {
                log.warn("Monitoring job {} failed", jobId, ex);
                failOrRetry(jobId, ex);
            }
        } finally {
            resetStillProcessing(jobId);
            queueService.releaseLock(lockKey);
        }
    }

    public Optional<MonitoredListing> claim(UUID jobId) {
        return transactionTemplate.execute(status -> {
            MonitoringJob job = jobRepository.findWithLockById(jobId).orElse(null);
            if (job == null) {
                return Optional.empty();
            }
            if (job.getStatus() != MonitoringJobStatus.PENDING && job.getStatus() != MonitoringJobStatus.RETRYING) {
                return Optional.empty();
            }
            if (job.getAvailableAt().isAfter(OffsetDateTime.now())) {
                return Optional.empty();
            }
            job.setStatus(MonitoringJobStatus.PROCESSING);
            job.setStartedAt(OffsetDateTime.now());
            job.setAttempts(job.getAttempts() + 1);
            jobRepository.save(job);
            return Optional.of(job.getMonitoredListing());
        });
    }

    public void complete(UUID jobId) {
        transactionTemplate.executeWithoutResult(status -> {
            jobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(MonitoringJobStatus.COMPLETED);
                job.setCompletedAt(OffsetDateTime.now());
                jobRepository.save(job);
            });
        });
    }

    public void failOrRetry(UUID jobId, Exception ex) {
        transactionTemplate.executeWithoutResult(status -> {
            jobRepository.findById(jobId).ifPresent(job -> {
                String message = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                job.setLastError(message.length() > 2000 ? message.substring(0, 2000) : message);
                if (job.getAttempts() >= job.getMaxAttempts()) {
                    job.setStatus(MonitoringJobStatus.FAILED);
                    job.setCompletedAt(OffsetDateTime.now());
                    queueService.pushDeadLetter(job.getId());
                    scheduleNextCheck(job);
                } else {
                    long delayMultiplier = Math.max(1, job.getAttempts());
                    job.setStatus(MonitoringJobStatus.RETRYING);
                    job.setAvailableAt(OffsetDateTime.now().plus(properties.getRetryBaseDelay().multipliedBy(delayMultiplier)));
                    queueService.requeue(job.getId());
                }
                jobRepository.save(job);
            });
        });
    }

    public void requeueForEtsyOutage(UUID jobId, Exception ex) {
        transactionTemplate.executeWithoutResult(status -> {
            jobRepository.findById(jobId).ifPresent(job -> {
                String message = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                job.setLastError(message.length() > 2000 ? message.substring(0, 2000) : message);
                if (job.getAttempts() >= job.getMaxAttempts()) {
                    job.setStatus(MonitoringJobStatus.FAILED);
                    job.setCompletedAt(OffsetDateTime.now());
                    queueService.pushDeadLetter(job.getId());
                    scheduleNextCheck(job);
                } else {
                    job.setStatus(MonitoringJobStatus.RETRYING);
                    job.setAvailableAt(OffsetDateTime.now().plusMinutes(15));
                    queueService.requeue(job.getId());
                }
                jobRepository.save(job);
            });
        });
    }

    private void resetStillProcessing(UUID jobId) {
        transactionTemplate.executeWithoutResult(status -> {
            jobRepository.findById(jobId).ifPresent(job -> {
                if (job.getStatus() == MonitoringJobStatus.PROCESSING) {
                    job.setStatus(MonitoringJobStatus.PENDING);
                    jobRepository.save(job);
                    queueService.requeue(job.getId());
                }
            });
        });
    }

    private void scheduleNextCheck(MonitoringJob job) {
        MonitoredListing listing = job.getMonitoredListing();
        if (listing == null || !Boolean.TRUE.equals(listing.getMonitoringEnabled())) {
            return;
        }
        listing.setNextCheckAt(planService.nextCheckAt(listing.getUser(), OffsetDateTime.now()));
        monitoredListingRepository.save(listing);
    }

}
