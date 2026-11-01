package com.listifyai.repository;

import com.listifyai.entity.AnalyzedListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalyzedListingRepository extends JpaRepository<AnalyzedListing, UUID> {
    long countByUserId(UUID userId);
}
