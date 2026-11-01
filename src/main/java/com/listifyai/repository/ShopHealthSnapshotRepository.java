package com.listifyai.repository;

import com.listifyai.entity.ShopHealthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShopHealthSnapshotRepository extends JpaRepository<ShopHealthSnapshot, UUID> {
    Optional<ShopHealthSnapshot> findByUserId(UUID userId);
    Optional<ShopHealthSnapshot> findFirstByUserIdOrderByComputedAtDesc(UUID userId);
}
