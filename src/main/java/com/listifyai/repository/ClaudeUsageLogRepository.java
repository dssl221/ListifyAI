package com.listifyai.repository;

import com.listifyai.entity.ClaudeUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ClaudeUsageLogRepository extends JpaRepository<ClaudeUsageLog, UUID> {

    @Query("SELECT SUM(c.inputTokens + c.outputTokens) FROM ClaudeUsageLog c WHERE c.createdAt >= :since")
    Long sumTokensSince(@Param("since") OffsetDateTime since);

    @Query("SELECT SUM(c.inputTokens) FROM ClaudeUsageLog c WHERE c.createdAt >= :since")
    Long sumInputTokensSince(@Param("since") OffsetDateTime since);

    @Query("SELECT SUM(c.outputTokens) FROM ClaudeUsageLog c WHERE c.createdAt >= :since")
    Long sumOutputTokensSince(@Param("since") OffsetDateTime since);

    @Query("SELECT COUNT(c) FROM ClaudeUsageLog c WHERE c.createdAt >= :since")
    Long countSince(@Param("since") OffsetDateTime since);

    @Query("""
            SELECT c.callType, COUNT(c), SUM(c.inputTokens), SUM(c.outputTokens)
              FROM ClaudeUsageLog c
             WHERE c.createdAt >= :since
             GROUP BY c.callType
            """)
    List<Object[]> usageByCallTypeSince(@Param("since") OffsetDateTime since);

    @Query("SELECT SUM(c.inputTokens + c.outputTokens) FROM ClaudeUsageLog c WHERE c.user.id = :userId")
    Long sumTokensByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT c.user.id, c.user.email, SUM(c.inputTokens + c.outputTokens)
              FROM ClaudeUsageLog c
             WHERE c.createdAt >= :since AND c.user IS NOT NULL
             GROUP BY c.user.id, c.user.email
             ORDER BY SUM(c.inputTokens + c.outputTokens) DESC
            """)
    List<Object[]> topUsersByTokensSince(@Param("since") OffsetDateTime since);
}
