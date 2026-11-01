package com.listifyai.repository;

import com.listifyai.entity.MonitoredListing;
import com.listifyai.monitoring.MonitoringEnums.MonitoredListingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitoredListingRepository extends JpaRepository<MonitoredListing, UUID> {
    @EntityGraph(attributePaths = "user")
    Optional<MonitoredListing> findByUserIdAndEtsyListingId(UUID userId, String etsyListingId);
    Optional<MonitoredListing> findByIdAndUserId(UUID id, UUID userId);
    long countByUserId(UUID userId);
    long countByMonitoringEnabledTrue();
    long countByStatus(MonitoredListingStatus status);
    long countByMonitoringEnabledTrueAndStatus(MonitoredListingStatus status);
    List<MonitoredListing> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<MonitoredListing> findTop20ByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = "user")
    List<MonitoredListing> findByMonitoringEnabledTrueAndStatusAndNextCheckAtLessThanEqual(
            MonitoredListingStatus status,
            OffsetDateTime nextCheckAt,
            Pageable pageable
    );
}
