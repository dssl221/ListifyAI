package com.listifyai.repository;

import com.listifyai.entity.MonitoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MonitoringEventRepository extends JpaRepository<MonitoringEvent, UUID> {

    long countByCreatedAtGreaterThanEqual(OffsetDateTime createdAt);

    List<MonitoringEvent> findByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(UUID userId, OffsetDateTime createdAt);

    @Query(value = """
            select event_type as rule_name, count(*) as event_count
              from monitoring_events
             where created_at >= now() - interval '7 days'
               and event_type like 'monitoring.finding.%'
             group by event_type
             order by event_count desc
             limit 5
            """, nativeQuery = true)
    List<Object[]> topFailingRulesLast7Days();
}
