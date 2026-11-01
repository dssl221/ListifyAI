package com.listifyai.monitoring;

import com.listifyai.entity.MonitoredListing;
import com.listifyai.entity.MonitoringJob;
import com.listifyai.monitoring.MonitoringEnums.MonitoringJobStatus;
import com.listifyai.monitoring.MonitoringEnums.MonitoringJobType;
import com.listifyai.repository.MonitoringJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class MonitoringQueueService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringQueueService.class);

    private final MonitoringJobRepository jobRepository;
    private final Optional<StringRedisTemplate> redisTemplate;
    private final MonitoringProperties properties;

    public MonitoringQueueService(MonitoringJobRepository jobRepository,
                                  Optional<StringRedisTemplate> redisTemplate,
                                  MonitoringProperties properties) {
        this.jobRepository = jobRepository;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Transactional
    public MonitoringJob enqueueListingCheck(MonitoredListing listing, String triggerType, int priority) {
        String idempotencySuffix = isManualTrigger(triggerType)
                ? UUID.randomUUID().toString()
                : listing.getNextCheckAt().toLocalDate().toString();
        String idempotencyKey = "listing-check:" + listing.getId() + ":" + idempotencySuffix;
        MonitoringJob job = jobRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (job == null) {
            job = new MonitoringJob();
            job.setUser(listing.getUser());
            job.setMonitoredListing(listing);
            job.setJobType(MonitoringJobType.LISTING_CHECK);
            job.setStatus(MonitoringJobStatus.PENDING);
            job.setPriority(priority);
            job.setIdempotencyKey(idempotencyKey);
            job.setMaxAttempts(properties.getMaxAttempts());
            OffsetDateTime now = OffsetDateTime.now();
            job.setScheduledAt(now);
            job.setAvailableAt(now);
            job.setPayload("{\"reason\":\"" + sanitize(triggerType) + "\"}");
            try {
                job = jobRepository.saveAndFlush(job);
            } catch (DataIntegrityViolationException ex) {
                job = jobRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> ex);
            }
        }
        push(job.getId());
        return job;
    }

    public Optional<UUID> pollJobId() {
        if (isRedisEnabled()) {
            try {
                String raw = redisTemplate.get().opsForList().rightPop(properties.getQueueKey(), Duration.ofSeconds(1));
                if (raw != null && !raw.isBlank()) {
                    return Optional.of(UUID.fromString(raw));
                }
            } catch (IllegalArgumentException ex) {
                log.warn("Discarded malformed monitoring job id from Redis queue", ex);
            } catch (RedisConnectionFailureException ex) {
                log.warn("Redis monitoring queue unavailable, falling back to database polling");
            }
        }

        return jobRepository
                .findByStatusInAndAvailableAtLessThanEqualOrderByPriorityAscCreatedAtAsc(
                        List.of(MonitoringJobStatus.PENDING, MonitoringJobStatus.RETRYING),
                        OffsetDateTime.now(),
                        PageRequest.of(0, 1)
                )
                .stream()
                .map(MonitoringJob::getId)
                .findFirst();
    }

    public boolean acquireLock(String key, Duration ttl) {
        if (!isRedisEnabled()) {
            return true;
        }
        try {
            Boolean acquired = redisTemplate.get().opsForValue().setIfAbsent(key, "1", ttl);
            return Boolean.TRUE.equals(acquired);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis lock unavailable for key {}; continuing with database lock", key);
            return true;
        }
    }

    public void releaseLock(String key) {
        if (!isRedisEnabled()) {
            return;
        }
        try {
            redisTemplate.get().delete(key);
        } catch (RedisConnectionFailureException ex) {
            log.debug("Could not release Redis lock {}", key, ex);
        }
    }

    public void pushDeadLetter(UUID jobId) {
        if (!isRedisEnabled()) {
            return;
        }
        try {
            redisTemplate.get().opsForList().leftPush(properties.getDeadLetterQueueKey(), jobId.toString());
        } catch (RedisConnectionFailureException ex) {
            log.warn("Could not push monitoring job {} to Redis DLQ", jobId, ex);
        }
    }

    public void requeue(UUID jobId) {
        push(jobId);
    }

    private void push(UUID jobId) {
        if (!isRedisEnabled()) {
            return;
        }
        try {
            redisTemplate.get().opsForList().leftPush(properties.getQueueKey(), jobId.toString());
        } catch (RedisConnectionFailureException ex) {
            log.warn("Could not enqueue monitoring job {} in Redis; database polling will recover it", jobId, ex);
        }
    }

    private boolean isRedisEnabled() {
        return properties.isRedisQueueEnabled() && redisTemplate.isPresent();
    }

    private boolean isManualTrigger(String triggerType) {
        return "user_enabled".equals(triggerType) || "manual".equals(triggerType);
    }

    private String sanitize(String value) {
        if (value == null) {
            return "system";
        }
        return value.replace("\\", "").replace("\"", "").trim();
    }
}
