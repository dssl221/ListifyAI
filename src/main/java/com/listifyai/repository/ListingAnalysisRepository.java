package com.listifyai.repository;

import com.listifyai.entity.ListingAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ListingAnalysisRepository extends JpaRepository<ListingAnalysis, UUID> {
    Page<ListingAnalysis> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
