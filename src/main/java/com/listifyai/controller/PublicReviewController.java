package com.listifyai.controller;

import com.listifyai.dto.PublicStatsDTO;
import com.listifyai.dto.ReviewRequestDTO;
import com.listifyai.dto.ReviewResponseDTO;
import com.listifyai.entity.Review;
import com.listifyai.repository.ReviewRepository;
import com.listifyai.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicReviewController {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public PublicReviewController(UserRepository userRepository, ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/stats")
    public PublicStatsDTO stats() {
        return new PublicStatsDTO(userRepository.count(), reviewRepository.countByApprovedTrue());
    }

    @GetMapping("/reviews")
    public List<ReviewResponseDTO> reviews(@RequestParam(defaultValue = "6") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 12));
        return reviewRepository.findByApprovedTrueOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponseDTO> createReview(@Valid @RequestBody ReviewRequestDTO request) {
        Review review = new Review();
        review.setDisplayName(request.displayName().trim());
        review.setSellerRole(request.sellerRole().trim());
        review.setRating(request.rating());
        review.setBody(request.body().trim());
        review.setApproved(true);

        Review saved = reviewRepository.save(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    private ReviewResponseDTO toResponse(Review review) {
        return new ReviewResponseDTO(
                review.getId(),
                review.getDisplayName(),
                review.getSellerRole(),
                review.getRating(),
                review.getBody(),
                review.getCreatedAt()
        );
    }
}
