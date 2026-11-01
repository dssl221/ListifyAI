package com.listifyai.repository;

import com.listifyai.entity.MonitoringRecommendation;
import com.listifyai.monitoring.MonitoringEnums.MonitoringRecommendationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitoringRecommendationRepository extends JpaRepository<MonitoringRecommendation, UUID> {
    Optional<MonitoringRecommendation> findByDedupeKey(String dedupeKey);
    List<MonitoringRecommendation> findByMonitoredListingIdAndUserIdAndStatusInOrderByDetectedAtDesc(
            UUID monitoredListingId,
            UUID userId,
            Collection<MonitoringRecommendationStatus> statuses,
            Pageable pageable
    );
    List<MonitoringRecommendation> findByAiCandidateTrueAndStatusOrderByOpportunityScoreDesc(
            MonitoringRecommendationStatus status,
            Pageable pageable
    );
    Optional<MonitoringRecommendation> findByIdAndUserId(UUID id, UUID userId);
    long countByMonitoredListingIdAndStatusIn(UUID monitoredListingId, Collection<MonitoringRecommendationStatus> statuses);
}
