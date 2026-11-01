package com.listifyai.repository;

import com.listifyai.entity.ListingSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingSnapshotRepository extends JpaRepository<ListingSnapshot, UUID> {
    Optional<ListingSnapshot> findFirstByMonitoredListingIdOrderByCapturedAtDesc(UUID monitoredListingId);
    List<ListingSnapshot> findByMonitoredListingIdOrderByCapturedAtDesc(UUID monitoredListingId, Pageable pageable);
    List<ListingSnapshot> findByMonitoredListingIdAndUserIdOrderByCapturedAtDesc(UUID monitoredListingId, UUID userId, Pageable pageable);
}
