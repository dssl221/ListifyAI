package com.listifyai.repository;

import com.listifyai.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {

    Page<Listing> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Listing> findByUserIdAndIsSavedTrue(UUID userId);

    long countByUserIdAndCreatedAtAfter(UUID userId, Instant since);

    long countByUserId(UUID userId);

    @Query(value = """
            select cast(created_at as date) as d, count(*)
              from listings
             where created_at >= :since
             group by cast(created_at as date)
             order by d
            """, nativeQuery = true)
    List<Object[]> countListingsByDaySince(@Param("since") OffsetDateTime since);
}
