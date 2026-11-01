package com.listifyai.repository;

import com.listifyai.entity.CachedAiOutput;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface CachedAiOutputRepository extends JpaRepository<CachedAiOutput, UUID> {
    
    @Query("SELECT c FROM CachedAiOutput c WHERE c.etsyListingId = :listingId AND c.marketplace = :marketplace AND c.section = :section AND c.promptVersion = :promptVersion AND c.modelVersion = :modelVersion AND c.validatorVersion = :validatorVersion AND c.schemaVersion = :schemaVersion AND c.cacheKeyVersion = :cacheKeyVersion AND c.inputHash = :inputHash AND c.expiresAt > :now ORDER BY c.createdAt DESC LIMIT 1")
    Optional<CachedAiOutput> findValidCache(
            @Param("listingId") String listingId,
            @Param("marketplace") String marketplace,
            @Param("section") String section,
            @Param("promptVersion") String promptVersion,
            @Param("modelVersion") String modelVersion,
            @Param("validatorVersion") String validatorVersion,
            @Param("schemaVersion") String schemaVersion,
            @Param("cacheKeyVersion") String cacheKeyVersion,
            @Param("inputHash") String inputHash,
            @Param("now") OffsetDateTime now
    );

    @Modifying
    @Query("UPDATE CachedAiOutput c SET c.expiresAt = :now WHERE c.etsyListingId = :listingId AND c.marketplace = :marketplace AND c.promptVersion = :promptVersion")
    void invalidateCache(
            @Param("listingId") String listingId,
            @Param("marketplace") String marketplace,
            @Param("promptVersion") String promptVersion,
            @Param("now") OffsetDateTime now
    );
}
