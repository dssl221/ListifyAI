package com.listifyai.controller;

import com.listifyai.dto.MonitoringStatsResponse;
import com.listifyai.monitoring.MonitoringEnums.MonitoringJobStatus;
import com.listifyai.repository.MonitoringEventRepository;
import com.listifyai.repository.MonitoringJobRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/admin/monitoring")
public class MonitoringStatsController {

    private final MonitoringJobRepository jobRepository;
    private final MonitoringEventRepository eventRepository;

    public MonitoringStatsController(MonitoringJobRepository jobRepository,
                                     MonitoringEventRepository eventRepository) {
        this.jobRepository = jobRepository;
        this.eventRepository = eventRepository;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public MonitoringStatsResponse stats() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime today = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
        return new MonitoringStatsResponse(
                jobRepository.countByStatus(MonitoringJobStatus.PENDING),
                jobRepository.countByStatus(MonitoringJobStatus.DEAD_LETTER),
                jobRepository.countByStatus(MonitoringJobStatus.PROCESSING),
                jobRepository.avgProcessingMsOfLast100Completed(),
                jobRepository.findLastCompletedAt().orElse(null),
                eventRepository.countByCreatedAtGreaterThanEqual(today)
        );
    }
}
