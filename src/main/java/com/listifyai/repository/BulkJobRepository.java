package com.listifyai.repository;

import com.listifyai.entity.BulkJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface BulkJobRepository extends JpaRepository<BulkJob, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BulkJob b WHERE b.id = :id")
    Optional<BulkJob> findJobForUpdate(@Param("id") UUID id);

    List<BulkJob> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<BulkJob> findByUserIdAndStatus(UUID userId, String status);

    List<BulkJob> findByStatusIn(Collection<String> statuses);

    long countByUserId(UUID userId);

    long countByStatus(String status);
}
