package com.listifyai.repository;

import com.listifyai.entity.BulkJobItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface BulkJobItemRepository extends JpaRepository<BulkJobItem, UUID> {

    @Transactional
    @Query(value = """
        UPDATE bulk_job_items
        SET status = 'RUNNING', locked_by = :workerId, locked_at = NOW(), attempt_count = attempt_count + 1
        WHERE id IN (
            SELECT id FROM bulk_job_items
            WHERE status IN ('PENDING', 'RETRYING')
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        )
        RETURNING *
        """, nativeQuery = true)
    List<BulkJobItem> claimNextItems(@Param("workerId") String workerId, @Param("limit") int limit);

    List<BulkJobItem> findByJobId(UUID jobId);

    @Modifying
    @Query(value = """
        UPDATE bulk_job_items 
        SET status = CASE WHEN attempt_count < 3 THEN 'RETRYING' ELSE 'DEAD_LETTER' END, 
            locked_by = NULL,
            last_error = CASE WHEN attempt_count >= 3 THEN 'Max retries reached during recovery' ELSE last_error END
        WHERE status = 'RUNNING' AND locked_at < NOW() - INTERVAL '5 minutes'
        """, nativeQuery = true)
    int recoverStuckItems();
}
