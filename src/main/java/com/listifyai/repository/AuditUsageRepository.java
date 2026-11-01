package com.listifyai.repository;

import com.listifyai.entity.AuditUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AuditUsageRepository extends JpaRepository<AuditUsage, UUID> {
    Optional<AuditUsage> findByUserIdAndPeriodStart(UUID userId, LocalDate periodStart);
}
