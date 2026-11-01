package com.listifyai.controller;

import com.listifyai.repository.CachedAiOutputRepository;
import com.listifyai.security.AdminOnly;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/cache")
@AdminOnly
public class AdminCacheController {

    private final CachedAiOutputRepository cachedAiOutputRepository;

    public AdminCacheController(CachedAiOutputRepository cachedAiOutputRepository) {
        this.cachedAiOutputRepository = cachedAiOutputRepository;
    }

    @PostMapping("/invalidate")
    @Transactional
    public ResponseEntity<Void> invalidateCache(
            @RequestParam String listingId,
            @RequestParam String marketplace,
            @RequestParam String promptVersion) {

        cachedAiOutputRepository.invalidateCache(listingId, marketplace, promptVersion, OffsetDateTime.now());
        return ResponseEntity.ok().build();
    }
}
