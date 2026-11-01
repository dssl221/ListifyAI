package com.listifyai.repository;

import com.listifyai.entity.UserUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface UserUsageRepository extends JpaRepository<UserUsage, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO user_usages (id, user_id, period_start, period_end, used_count, plan_limit, created_at, updated_at)
            SELECT gen_random_uuid(), CAST(:userId AS uuid), CAST(:start AS date), CAST(:end AS date), 0, :limit, now(), now()
            WHERE NOT EXISTS (
                SELECT 1 FROM user_usages 
                WHERE user_id = CAST(:userId AS uuid) AND period_start = CAST(:start AS date) AND period_end = CAST(:end AS date)
            )
            """, nativeQuery = true)
    void ensureUsageRecord(@Param("userId") UUID userId, 
                           @Param("start") LocalDate start, 
                           @Param("end") LocalDate end, 
                           @Param("limit") int limit);

    @Modifying
    @Query("""
            UPDATE UserUsage u
               SET u.usedCount = u.usedCount + 1
             WHERE u.user.id = :userId
               AND u.periodStart = :start
               AND u.periodEnd = :end
               AND u.usedCount < :limit
            """)
    int incrementUsageAtomically(@Param("userId") UUID userId, 
                                 @Param("start") LocalDate start, 
                                 @Param("end") LocalDate end, 
                                 @Param("limit") int limit);

    java.util.Optional<UserUsage> findByUserIdAndPeriodStartAndPeriodEnd(UUID userId, LocalDate start, LocalDate end);
}
