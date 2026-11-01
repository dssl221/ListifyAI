package com.listifyai.controller;

import com.listifyai.dto.GenerationResultDTO;
import com.listifyai.dto.ListingDetailDTO;
import com.listifyai.dto.ListingHistoryDTO;
import com.listifyai.dto.DisplayNameRequestDTO;
import com.listifyai.dto.ProductInputDTO;
import com.listifyai.dto.UserProfileDTO;
import com.listifyai.entity.User;
import com.listifyai.security.AuthUtils;
import com.listifyai.service.ListingService;
import com.listifyai.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class GeneratorController {

    private final ListingService listingService;
    private final UserService    userService;

    public GeneratorController(ListingService listingService, UserService userService) {
        this.listingService = listingService;
        this.userService    = userService;
    }

    /**
     * POST /api/generate
     * Generates an Etsy listing, Shopify listing, or both in parallel.
     * Returns 402 if the user has exhausted their monthly quota.
     */
    @PostMapping("/generate")
    public ResponseEntity<GenerationResultDTO> generate(
            @Valid @RequestBody ProductInputDTO input) {

        User user = AuthUtils.getCurrentUser();
        GenerationResultDTO result = listingService.generate(input, user);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/listings?page=0&size=20
     * Paginated listing history for the authenticated user, newest first.
     */
    @GetMapping("/listings")
    public ResponseEntity<Page<ListingHistoryDTO>> getListings(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = AuthUtils.getCurrentUser();
        int maxHistorySize = getMaxHistorySize(user.getPlan());
        long offset = (long) page * size;
        int effectiveSize = maxHistorySize == Integer.MAX_VALUE
                ? size
                : (int) Math.min(size, maxHistorySize - offset);

        if (effectiveSize <= 0) {
            return ResponseEntity.ok(Page.empty(PageRequest.of(page, size)));
        }

        return ResponseEntity.ok(listingService.getHistory(user, page, size, effectiveSize, maxHistorySize));
    }

    private int getMaxHistorySize(String plan) {
        return switch (plan != null ? plan.toLowerCase() : "free") {
            case "free" -> 10;
            case "starter" -> 10;
            case "growth" -> 50;
            case "pro" -> Integer.MAX_VALUE;
            default -> 10;
        };
    }

    @GetMapping("/listings/{id}")
    public ResponseEntity<ListingDetailDTO> getListing(@PathVariable UUID id) {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(listingService.getDetail(id, user));
    }

    /**
     * PUT /api/listings/{id}/save
     * Toggles the saved state of a listing.
     * Returns 403 if the listing does not belong to the current user.
     */
    @PutMapping("/listings/{id}/save")
    public ResponseEntity<Void> toggleSave(@PathVariable UUID id) {
        User user = AuthUtils.getCurrentUser();
        listingService.toggleSave(id, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/listings/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable UUID id) {
        User user = AuthUtils.getCurrentUser();
        listingService.deleteListing(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/me
     * Returns the authenticated user's profile including plan and usage stats.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> me() {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(userService.getProfile(user));
    }

    @PutMapping("/me/display-name")
    public ResponseEntity<UserProfileDTO> updateDisplayName(
            @Valid @RequestBody DisplayNameRequestDTO request) {

        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(userService.updateDisplayName(user, request.displayName()));
    }
}
