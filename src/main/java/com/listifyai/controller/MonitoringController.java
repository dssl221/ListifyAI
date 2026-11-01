package com.listifyai.controller;

import com.listifyai.dto.MonitoringDtos.*;
import com.listifyai.entity.MonitoringRecommendation;
import com.listifyai.entity.User;
import com.listifyai.exception.ListingLimitException;
import com.listifyai.monitoring.ListingMonitoringService;
import com.listifyai.monitoring.MonitoringAiFixService;
import com.listifyai.security.AuthUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private static final Map<String, Integer> MAX_MONITORED_LISTINGS = Map.of(
            "free",    3,
            "starter", 10,
            "growth",  30,
            "pro",     100
    );

    private final ListingMonitoringService monitoringService;
    private final MonitoringAiFixService monitoringAiFixService;

    public MonitoringController(ListingMonitoringService monitoringService,
                                MonitoringAiFixService monitoringAiFixService) {
        this.monitoringService = monitoringService;
        this.monitoringAiFixService = monitoringAiFixService;
    }

    @PostMapping("/enable")
    public ResponseEntity<MonitoredListingDTO> enable(@Valid @RequestBody EnableMonitoringRequest request) {
        User user = AuthUtils.getCurrentUser();
        String plan = user.getPlan() != null ? user.getPlan().toLowerCase() : "free";
        int max = MAX_MONITORED_LISTINGS.getOrDefault(plan, 3);
        long current = monitoringService.countMonitoredListings(user);
        if (current >= max) {
            throw new ListingLimitException(
                    "You have reached the monitored listing limit for your " + plan +
                            " plan (" + max + " listings). Upgrade to monitor more listings."
            );
        }
        return ResponseEntity.ok(monitoringService.enable(request.url(), user));
    }

    @PostMapping("/disable")
    public ResponseEntity<MonitoredListingDTO> disable(@Valid @RequestBody DisableMonitoringRequest request) {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(monitoringService.disable(parseUuid(request.listingId()), user));
    }

    @GetMapping("/listings")
    public ResponseEntity<List<MonitoredListingDTO>> listings() {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(monitoringService.listings(user));
    }

    @GetMapping("/history/{listingId}")
    public ResponseEntity<List<ListingSnapshotDTO>> history(@PathVariable String listingId) {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(monitoringService.history(parseUuid(listingId), user));
    }

    @GetMapping("/recommendations/{listingId}")
    public ResponseEntity<List<MonitoringRecommendationDTO>> recommendations(@PathVariable String listingId) {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(monitoringService.recommendations(parseUuid(listingId), user));
    }

    @PostMapping("/recommendations/{recommendationId}/ai-fix")
    public ResponseEntity<MonitoringRecommendationDTO> generateAiFix(@PathVariable String recommendationId) {
        User user = AuthUtils.getCurrentUser();
        MonitoringRecommendation updated = monitoringAiFixService.generateFixForRecommendation(
                parseUuid(recommendationId),
                user.getId()
        );
        return ResponseEntity.ok(toRecommendationDto(updated));
    }

    private MonitoringRecommendationDTO toRecommendationDto(MonitoringRecommendation recommendation) {
        return new MonitoringRecommendationDTO(
                recommendation.getId(),
                recommendation.getMonitoredListing().getId(),
                recommendation.getSnapshot() == null ? null : recommendation.getSnapshot().getId(),
                recommendation.getRecommendationType(),
                recommendation.getStatus().name(),
                recommendation.getSeverity().name(),
                recommendation.getOpportunityScore(),
                recommendation.getTitle(),
                recommendation.getMessage(),
                recommendation.getAiFixText(),
                Boolean.TRUE.equals(recommendation.getAiCandidate()),
                recommendation.getDetectedAt().toInstant()
        );
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid listing id.");
        }
    }
}
