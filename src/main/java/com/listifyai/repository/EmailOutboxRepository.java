package com.listifyai.repository;
import com.listifyai.entity.EmailOutbox;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, UUID> {
    Optional<EmailOutbox> findByIdempotencyKey(String key);
    long countByStatus(String status);
    @Transactional
    @Query(value = """
      UPDATE email_outbox SET status='PROCESSING', locked_by=:workerId, locked_at=NOW(), attempts=attempts+1
      WHERE id IN (SELECT id FROM email_outbox WHERE status IN ('PENDING','RETRYING') AND next_attempt_at <= NOW()
                   ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED) RETURNING *
      """, nativeQuery = true)
    List<EmailOutbox> claimDue(@Param("workerId") String workerId, @Param("limit") int limit);
    @Modifying @Query(value = """
      UPDATE email_outbox SET status='RETRYING', locked_by=NULL, locked_at=NULL, next_attempt_at=NOW(), last_error='Recovered after worker interruption'
      WHERE status='PROCESSING' AND locked_at < NOW() - INTERVAL '10 minutes'
      """, nativeQuery = true)
    int recoverStuck();
}
