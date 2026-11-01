package com.listifyai.controller;

import com.listifyai.dto.EtsyAuditDtos.*;
import com.listifyai.entity.User;
import com.listifyai.security.AuthUtils;
import com.listifyai.service.AuditUsageService;
import com.listifyai.service.EtsyListingAuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EtsyAuditController {

    private final EtsyListingAuditService auditService;
    private final AuditUsageService auditUsageService;

    public EtsyAuditController(EtsyListingAuditService auditService, AuditUsageService auditUsageService) {
        this.auditService = auditService;
        this.auditUsageService = auditUsageService;
    }

    @PostMapping("/analyze-etsy-listing")
    public ResponseEntity<ListingAuditDTO> analyze(@Valid @RequestBody AnalyzeRequest request) {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(auditService.analyze(request.url(), user));
    }

    @PostMapping("/regenerate-title")
    public ResponseEntity<SectionRegenerationDTO> regenerateTitle(@Valid @RequestBody RegenerateRequest request) {
        return regenerate(request, "title");
    }

    @PostMapping("/regenerate-tags")
    public ResponseEntity<SectionRegenerationDTO> regenerateTags(@Valid @RequestBody RegenerateRequest request) {
        return regenerate(request, "tags");
    }

    @PostMapping("/regenerate-description")
    public ResponseEntity<SectionRegenerationDTO> regenerateDescription(@Valid @RequestBody RegenerateRequest request) {
        return regenerate(request, "description");
    }

    @GetMapping("/audit-history")
    public ResponseEntity<Page<HistoryItemDTO>> auditHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(auditService.history(user, page, size));
    }

    @GetMapping("/audit-usage")
    public ResponseEntity<AuditUsageDTO> auditUsage() {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(auditUsageService.getUsage(user));
    }

    private ResponseEntity<SectionRegenerationDTO> regenerate(RegenerateRequest request, String section) {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(auditService.regenerate(parseUuid(request.analysisId()), section, user));
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid audit id.");
        }
    }
}
