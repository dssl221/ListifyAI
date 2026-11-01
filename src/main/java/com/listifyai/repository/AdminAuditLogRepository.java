package com.listifyai.repository;

import com.listifyai.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {
    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AdminAuditLog> findByAdminUserId(UUID adminUserId, Pageable pageable);
}
