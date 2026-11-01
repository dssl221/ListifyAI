package com.listifyai.repository;

import com.listifyai.entity.MonitoringJob;
import com.listifyai.monitoring.MonitoringEnums.MonitoringJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitoringJobRepository extends JpaRepository<MonitoringJob, UUID> {
    Optional<MonitoringJob> findByIdempotencyKey(String idempotencyKey);
    List<MonitoringJob> findByStatusInAndAvailableAtLessThanEqualOrderByPriorityAscCreatedAtAsc(
            Collection<MonitoringJobStatus> statuses,
            OffsetDateTime availableAt,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from MonitoringJob j where j.id = :id")
    Optional<MonitoringJob> findWithLockById(@Param("id") UUID id);

    long countByStatus(MonitoringJobStatus status);

    long countByCreatedAtAfter(OffsetDateTime since);

    long countByStatusAndCreatedAtAfter(MonitoringJobStatus status, OffsetDateTime since);

    List<MonitoringJob> findTop20ByStatusOrderByCreatedAtDesc(MonitoringJobStatus status);

    @Query(value = """
            select coalesce(avg(extract(epoch from (completed_at - started_at)) * 1000), 0)
              from (
                    select started_at, completed_at
                      from monitoring_jobs
                     where status = 'COMPLETED'
                       and started_at is not null
                       and completed_at is not null
                     order by completed_at desc
                     limit 100
                   ) recent_jobs
            """, nativeQuery = true)
    double avgProcessingMsOfLast100Completed();

    @Query(value = """
            select completed_at
              from monitoring_jobs
             where status = 'COMPLETED'
               and completed_at is not null
             order by completed_at desc
             limit 1
            """, nativeQuery = true)
    Optional<OffsetDateTime> findLastCompletedAt();
}
