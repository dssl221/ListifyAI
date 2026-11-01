package com.listifyai.monitoring;

import com.listifyai.entity.MonitoredListing;
import com.listifyai.monitoring.MonitoringEnums.MonitoredListingStatus;
import com.listifyai.repository.MonitoredListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class MonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitoringScheduler.class);

    private final MonitoredListingRepository monitoredListingRepository;
    private final MonitoringQueueService queueService;
    private final MonitoringPlanService planService;
    private final MonitoringProperties properties;
    private final MonitoringAiFixService monitoringAiFixService;

    public MonitoringScheduler(MonitoredListingRepository monitoredListingRepository,
                               MonitoringQueueService queueService,
                               MonitoringPlanService planService,
                               MonitoringProperties properties,
                               MonitoringAiFixService monitoringAiFixService) {
        this.monitoredListingRepository = monitoredListingRepository;
        this.queueService = queueService;
        this.planService = planService;
        this.properties = properties;
        this.monitoringAiFixService = monitoringAiFixService;
    }

    @Scheduled(cron = "${monitoring.scheduler-cron:0 */15 * * * *}")
    public void enqueueDueListings() {
        if (!properties.isEnabled()) {
            return;
        }
        List<MonitoredListing> due = monitoredListingRepository
                .findByMonitoringEnabledTrueAndStatusAndNextCheckAtLessThanEqual(
                        MonitoredListingStatus.ACTIVE,
                        OffsetDateTime.now(),
                        PageRequest.of(0, properties.getSchedulerBatchSize())
                );
        for (MonitoredListing listing : due) {
            try {
                queueService.enqueueListingCheck(listing, "scheduled", planService.priority(listing.getUser()));
            } catch (Exception ex) {
                log.warn("Could not enqueue monitoring listing {}", listing.getId(), ex);
            }
        }
    }

    @Scheduled(cron = "${monitoring.ai-fix-cron:0 */30 * * * *}")
    public void processAiFixes() {
        if (!properties.isEnabled()) {
            return;
        }
        monitoringAiFixService.processPendingAiFixes(20);
    }
}
