package com.listifyai.repository;

import com.listifyai.entity.RegenerationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegenerationHistoryRepository extends JpaRepository<RegenerationHistory, UUID> {
}
