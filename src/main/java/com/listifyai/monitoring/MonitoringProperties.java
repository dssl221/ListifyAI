package com.listifyai.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {

    private boolean enabled = true;
    private boolean redisQueueEnabled = false;
    private String queueKey = "tagwing:monitoring:queue";
    private String deadLetterQueueKey = "tagwing:monitoring:dead-letter";
    private int schedulerBatchSize = 100;
    private int workerBatchSize = 10;
    private int maxAttempts = 3;
    private Duration retryBaseDelay = Duration.ofMinutes(5);
    private Duration lockTtl = Duration.ofMinutes(10);
    private Duration starterFrequency = Duration.ofDays(7);
    private Duration growthFrequency = Duration.ofDays(3);
    private Duration proFrequency = Duration.ofDays(1);
    private Duration freeFrequency = Duration.ofDays(14);
    private int aiCandidateThreshold = 65;
    private int scoreDropThreshold = 10;
    private int staleListingDays = 45;
    private int unchangedListingDays = 30;
    private int optimizationStaleDays = 30;
    private Rules rules = new Rules();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isRedisQueueEnabled() { return redisQueueEnabled; }
    public void setRedisQueueEnabled(boolean redisQueueEnabled) { this.redisQueueEnabled = redisQueueEnabled; }
    public String getQueueKey() { return queueKey; }
    public void setQueueKey(String queueKey) { this.queueKey = queueKey; }
    public String getDeadLetterQueueKey() { return deadLetterQueueKey; }
    public void setDeadLetterQueueKey(String deadLetterQueueKey) { this.deadLetterQueueKey = deadLetterQueueKey; }
    public int getSchedulerBatchSize() { return schedulerBatchSize; }
    public void setSchedulerBatchSize(int schedulerBatchSize) { this.schedulerBatchSize = schedulerBatchSize; }
    public int getWorkerBatchSize() { return workerBatchSize; }
    public void setWorkerBatchSize(int workerBatchSize) { this.workerBatchSize = workerBatchSize; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public Duration getRetryBaseDelay() { return retryBaseDelay; }
    public void setRetryBaseDelay(Duration retryBaseDelay) { this.retryBaseDelay = retryBaseDelay; }
    public Duration getLockTtl() { return lockTtl; }
    public void setLockTtl(Duration lockTtl) { this.lockTtl = lockTtl; }
    public Duration getStarterFrequency() { return starterFrequency; }
    public void setStarterFrequency(Duration starterFrequency) { this.starterFrequency = starterFrequency; }
    public Duration getGrowthFrequency() { return growthFrequency; }
    public void setGrowthFrequency(Duration growthFrequency) { this.growthFrequency = growthFrequency; }
    public Duration getProFrequency() { return proFrequency; }
    public void setProFrequency(Duration proFrequency) { this.proFrequency = proFrequency; }
    public Duration getFreeFrequency() { return freeFrequency; }
    public void setFreeFrequency(Duration freeFrequency) { this.freeFrequency = freeFrequency; }
    public int getAiCandidateThreshold() { return aiCandidateThreshold; }
    public void setAiCandidateThreshold(int aiCandidateThreshold) { this.aiCandidateThreshold = aiCandidateThreshold; }
    public int getScoreDropThreshold() { return scoreDropThreshold; }
    public void setScoreDropThreshold(int scoreDropThreshold) { this.scoreDropThreshold = scoreDropThreshold; }
    public int getStaleListingDays() { return staleListingDays; }
    public void setStaleListingDays(int staleListingDays) { this.staleListingDays = staleListingDays; }
    public int getUnchangedListingDays() { return unchangedListingDays; }
    public void setUnchangedListingDays(int unchangedListingDays) { this.unchangedListingDays = unchangedListingDays; }
    public int getOptimizationStaleDays() { return optimizationStaleDays; }
    public void setOptimizationStaleDays(int optimizationStaleDays) { this.optimizationStaleDays = optimizationStaleDays; }
    public Rules getRules() { return rules; }
    public void setRules(Rules rules) { this.rules = rules; }

    public static class Rules {
        private String version = "1.1";

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }
}
