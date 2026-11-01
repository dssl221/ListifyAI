package com.listifyai.repository;

import com.listifyai.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query(value = """
            select count(*)
              from audit_log
             where action = 'claude.call'
               and created_at >= date_trunc('day', now())
            """, nativeQuery = true)
    long countClaudeCallsToday();
}
