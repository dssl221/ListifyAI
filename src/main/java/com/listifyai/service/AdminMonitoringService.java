package com.listifyai.service;

import com.listifyai.dto.AdminDtos.AdminMonitoringOverviewDTO;
import com.listifyai.entity.MonitoredListing;
import com.listifyai.entity.MonitoringJob;
import com.listifyai.entity.User;
import com.listifyai.monitoring.MonitoringEnums.MonitoredListingStatus;
import com.listifyai.monitoring.MonitoringEnums.MonitoringJobStatus;
import com.listifyai.repository.MonitoredListingRepository;
import com.listifyai.repository.MonitoringJobRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@SuppressWarnings({"null", "unchecked", "unused"})
public class AdminMonitoringService {

    private final MonitoredListingRepository monitoredListingRepository;
    private final MonitoringJobRepository monitoringJobRepository;
    private final AdminAuditService adminAuditService;

    public AdminMonitoringService(MonitoredListingRepository monitoredListingRepository,
                                  MonitoringJobRepository monitoringJobRepository,
                                  AdminAuditService adminAuditService) {
        this.monitoredListingRepository = monitoredListingRepository;
        this.monitoringJobRepository = monitoringJobRepository;
        this.adminAuditService = adminAuditService;
    }

    @Transactional(readOnly = true)
    public AdminMonitoringOverviewDTO getOverview() {
        OffsetDateTime today = OffsetDateTime.now().toLocalDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        long totalJobsToday = monitoringJobRepository.countByCreatedAtAfter(today);
        long failedJobsToday = monitoringJobRepository.countByStatusAndCreatedAtAfter(MonitoringJobStatus.FAILED, today);
        double successRate = totalJobsToday == 0 ? 100.0 : ((double) (totalJobsToday - failedJobsToday) / totalJobsToday) * 100.0;
        return new AdminMonitoringOverviewDTO(
                monitoredListingRepository.count(),
                monitoredListingRepository.countByStatus(MonitoredListingStatus.ACTIVE),
                monitoredListingRepository.countByStatus(MonitoredListingStatus.PAUSED),
                totalJobsToday,
                failedJobsToday,
                monitoringJobRepository.countByStatus(MonitoringJobStatus.PENDING),
                0,
                successRate
        );
    }

    public void retryFailedJob(UUID jobId, User admin, HttpServletRequest request) {
        MonitoringJob job = monitoringJobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitoring job not found"));
        job.setStatus(MonitoringJobStatus.PENDING);
        job.setLastError(null);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        monitoringJobRepository.save(job);
        adminAuditService.log(admin, "MONITORING_JOB_RETRIED", "monitoring_job", jobId, Map.of(), request);
    }

    public void pauseMonitoredListing(UUID listingId, User admin, HttpServletRequest request) {
        MonitoredListing listing = loadListing(listingId);
        listing.setMonitoringEnabled(false);
        listing.setStatus(MonitoredListingStatus.PAUSED);
        monitoredListingRepository.save(listing);
        adminAuditService.log(admin, "MONITORING_PAUSED", "monitored_listing", listingId, Map.of(), request);
    }

    public void resumeMonitoredListing(UUID listingId, User admin, HttpServletRequest request) {
        MonitoredListing listing = loadListing(listingId);
        listing.setMonitoringEnabled(true);
        listing.setStatus(MonitoredListingStatus.ACTIVE);
        monitoredListingRepository.save(listing);
        adminAuditService.log(admin, "MONITORING_RESUMED", "monitored_listing", listingId, Map.of(), request);
    }

    private MonitoredListing loadListing(UUID listingId) {
        return monitoredListingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitored listing not found"));
    }
}
