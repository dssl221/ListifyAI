package com.listifyai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.dto.EtsyOutputDTO;
import com.listifyai.dto.ShopHealthDTO;
import com.listifyai.dto.ShopHealthDTO.HealthSignalDTO;
import com.listifyai.dto.ShopifyOutputDTO;
import com.listifyai.entity.Listing;
import com.listifyai.entity.ShopHealthSnapshot;
import com.listifyai.entity.User;
import com.listifyai.repository.ListingRepository;
import com.listifyai.repository.ShopHealthSnapshotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class ShopHealthService {

    private static final Logger log = LoggerFactory.getLogger(ShopHealthService.class);
    private static final List<String> FALLBACK_FIXES = List.of(
            "Generate more listings in Tagwing so the health score can spot stronger patterns.",
            "Fill in the occasion, target buyer, and material fields on your next generation for richer analysis.",
            "Try generating for both platforms to see how Etsy and Shopify SEO differ for your products."
    );

    private final ListingRepository listingRepository;
    private final ShopHealthSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public ShopHealthService(ListingRepository listingRepository,
                             ShopHealthSnapshotRepository snapshotRepository,
                             ObjectMapper objectMapper) {
        this.listingRepository = listingRepository;
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ShopHealthDTO compute(User user) {
        List<Listing> listings = listingRepository
                .findByUserIdOrderByCreatedAtDesc(
                        user.getId(),
                        PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).getContent();

        if (listings.isEmpty()) {
            return emptyHealth();
        }

        List<EtsyOutputDTO> etsyOutputs = listings.stream()
                .filter(listing -> listing.getEtsyOutput() != null)
                .map(listing -> parseJson(listing.getEtsyOutput(), EtsyOutputDTO.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<ShopifyOutputDTO> shopifyOutputs = listings.stream()
                .filter(listing -> listing.getShopifyOutput() != null)
                .map(listing -> parseJson(listing.getShopifyOutput(), ShopifyOutputDTO.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<HealthSignalDTO> signals = new ArrayList<>();
        signals.add(computeQualityScoreSignal(etsyOutputs, shopifyOutputs));
        signals.add(computeTagSignal(etsyOutputs));
        signals.add(computeTitleConsistencySignal(etsyOutputs));
        signals.add(computePlatformCoverageSignal(listings));
        signals.add(computeToneConsistencySignal(listings));
        signals.add(computeShippingRiskSignal());

        int overallScore = (int) signals.stream()
                .mapToInt(HealthSignalDTO::score)
                .average()
                .orElse(50);

        String grade = overallScore >= 80 ? "A"
                : overallScore >= 65 ? "B"
                : overallScore >= 50 ? "C" : "D";
        String headline = buildHeadline(overallScore, signals);
        List<String> topFixes = buildTopFixes(signals);
        OffsetDateTime computedAt = OffsetDateTime.now();

        ShopHealthDTO dto = new ShopHealthDTO(
                overallScore,
                grade,
                headline,
                listings.size(),
                signals,
                topFixes,
                computedAt.toInstant()
        );

        ShopHealthSnapshot snapshot = snapshotRepository
                .findByUserId(user.getId())
                .orElse(new ShopHealthSnapshot());
        snapshot.setUser(user);
        snapshot.setOverallScore(overallScore);
        snapshot.setListingsAnalysed(listings.size());
        snapshot.setHealthJson(toJson(dto));
        snapshot.setComputedAt(computedAt);
        snapshotRepository.save(snapshot);

        return dto;
    }

    @Transactional(readOnly = true)
    public Optional<ShopHealthDTO> getCached(User user) {
        return snapshotRepository.findByUserId(user.getId())
                .map(snapshot -> parseJson(snapshot.getHealthJson(), ShopHealthDTO.class));
    }

    private HealthSignalDTO computeQualityScoreSignal(
            List<EtsyOutputDTO> etsyOutputs,
            List<ShopifyOutputDTO> shopifyOutputs) {

        List<Integer> scores = new ArrayList<>();
        etsyOutputs.stream()
                .filter(output -> output.qualityScore() > 0)
                .forEach(output -> scores.add(output.qualityScore()));
        shopifyOutputs.stream()
                .filter(output -> output.qualityScore() > 0)
                .forEach(output -> scores.add(output.qualityScore()));

        if (scores.isEmpty()) {
            return new HealthSignalDTO(
                    "Quality scores",
                    "needs_work",
                    40,
                    "No quality scores found in your listing history.",
                    "Generate a new listing to see quality scores."
            );
        }

        double avg = scores.stream().mapToInt(score -> score).average().orElse(0);
        int signal = (int) avg;
        long weakCount = scores.stream().filter(score -> score < 60).count();
        String status = avg >= 75 ? "strong" : avg >= 55 ? "needs_work" : "critical";
        String finding = String.format(
                "Average quality score across %d listings is %.0f/100. %d listings scored below 60.",
                scores.size(),
                avg,
                weakCount
        );
        String suggestion = weakCount > 0
                ? "Open the History page, expand each low-scoring listing, and follow its 3 improvement tips."
                : "Quality is strong. Keep using all 8 input fields for the best scores.";

        return new HealthSignalDTO("Quality scores", status, signal, finding, suggestion);
    }

    private HealthSignalDTO computeTagSignal(List<EtsyOutputDTO> etsyOutputs) {
        if (etsyOutputs.isEmpty()) {
            return new HealthSignalDTO(
                    "Etsy tags",
                    "needs_work",
                    50,
                    "No Etsy listings found to analyse tags.",
                    "Generate an Etsy listing to see tag health."
            );
        }

        long highCompDominated = etsyOutputs.stream()
                .filter(output -> {
                    if (output.tags() == null) {
                        return false;
                    }
                    long highCount = output.tags().stream()
                            .filter(tag -> "high".equals(tag.competition()))
                            .count();
                    return highCount > 8;
                })
                .count();

        long totalEtsy = etsyOutputs.size();
        int pct = (int) ((highCompDominated * 100) / totalEtsy);
        int score = Math.max(20, 100 - pct);
        String status = pct < 20 ? "strong" : pct < 50 ? "needs_work" : "critical";
        String finding = pct > 30
                ? String.format("%d%% of your Etsy listings rely mostly on high-competition tags that many sellers also use.", pct)
                : "Good tag mix - most listings balance broad and specific phrases.";
        String suggestion = pct > 30
                ? "In your next listings, include 4-5 long-tail phrases like 'handmade copper ring for nurse' instead of just 'ring'."
                : "Keep including niche and occasion-specific tags to stay ahead.";

        return new HealthSignalDTO("Etsy tag mix", status, score, finding, suggestion);
    }

    private HealthSignalDTO computeTitleConsistencySignal(List<EtsyOutputDTO> etsyOutputs) {
        if (etsyOutputs.isEmpty()) {
            return new HealthSignalDTO(
                    "Title consistency",
                    "needs_work",
                    50,
                    "No Etsy listings to analyse titles.",
                    "Generate Etsy listings to see title health."
            );
        }

        List<Integer> titleLengths = etsyOutputs.stream()
                .filter(output -> output.titles() != null && !output.titles().isEmpty())
                .map(output -> output.titles().get(0).length())
                .collect(Collectors.toList());

        if (titleLengths.isEmpty()) {
            return new HealthSignalDTO(
                    "Title consistency",
                    "needs_work",
                    50,
                    "Could not read title data.",
                    "Regenerate some listings."
            );
        }

        double avg = titleLengths.stream().mapToInt(length -> length).average().orElse(0);
        long shortCount = titleLengths.stream().filter(length -> length < 100).count();
        int score = avg >= 120 ? 90 : avg >= 100 ? 70 : 45;
        String status = avg >= 120 ? "strong" : avg >= 100 ? "needs_work" : "critical";
        String finding = String.format(
                "Average Etsy title length is %.0f chars across %d listings. %d listings use fewer than 100 chars, leaving search space unused.",
                avg,
                titleLengths.size(),
                shortCount
        );
        String suggestion = shortCount > 0
                ? "Expand short titles to 120-140 chars by adding material, buyer, and occasion phrases."
                : "Titles are using Etsy's character limit well.";

        return new HealthSignalDTO("Title length", status, score, finding, suggestion);
    }

    private HealthSignalDTO computePlatformCoverageSignal(List<Listing> listings) {
        long etsyCount = listings.stream()
                .filter(listing -> "etsy".equalsIgnoreCase(listing.getPlatform())
                        || "both".equalsIgnoreCase(listing.getPlatform()))
                .count();
        long shopifyCount = listings.stream()
                .filter(listing -> "shopify".equalsIgnoreCase(listing.getPlatform())
                        || "both".equalsIgnoreCase(listing.getPlatform()))
                .count();

        boolean bothUsed = etsyCount > 0 && shopifyCount > 0;
        int score = bothUsed ? 90 : 55;
        String status = bothUsed ? "strong" : "needs_work";
        String finding = bothUsed
                ? String.format("You generate for both platforms. %d Etsy and %d Shopify listings in history.", etsyCount, shopifyCount)
                : etsyCount > 0
                        ? "All listings are Etsy-only. Shopify has different SEO rules that could expand your reach."
                        : "All listings are Shopify-only. Etsy has a large discovery audience for handmade products.";
        String suggestion = bothUsed
                ? "Good platform spread. Keep cross-publishing your top products."
                : "Try generating a listing for both platforms to compare the copy differences.";

        return new HealthSignalDTO("Platform coverage", status, score, finding, suggestion);
    }

    private HealthSignalDTO computeToneConsistencySignal(List<Listing> listings) {
        Map<String, Long> toneCounts = listings.stream()
                .collect(Collectors.groupingBy(
                        listing -> listing.getTone() != null ? listing.getTone().toLowerCase() : "unknown",
                        Collectors.counting()
                ));

        String dominantTone = toneCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        long dominantCount = toneCounts.getOrDefault(dominantTone, 0L);
        int pct = (int) ((dominantCount * 100) / listings.size());
        int score = pct >= 60 ? 85 : pct >= 40 ? 70 : 50;
        String status = pct >= 40 ? "strong" : "needs_work";
        String finding = String.format(
                "%d%% of your listings use a '%s' tone. %d different tones used overall.",
                pct,
                dominantTone,
                toneCounts.size()
        );
        String suggestion = pct < 40
                ? "Pick 1-2 tones that match your brand and use them consistently across all products."
                : "Tone is consistent. " + dominantTone + " works well for building a recognisable brand voice.";

        return new HealthSignalDTO("Tone consistency", status, score, finding, suggestion);
    }

    private HealthSignalDTO computeShippingRiskSignal() {
        return new HealthSignalDTO(
                "Shipping risk",
                "strong",
                80,
                "Use the shipping price field when generating Etsy listings to check if your rate could affect search ranking.",
                "Add your shipping price to each Etsy generation to get a real-time warning."
        );
    }

    private String buildHeadline(int score, List<HealthSignalDTO> signals) {
        long critical = signals.stream()
                .filter(signal -> "critical".equals(signal.status()))
                .count();
        long needsWork = signals.stream()
                .filter(signal -> "needs_work".equals(signal.status()))
                .count();

        if (score >= 80) {
            return "Your listing workflow is in good shape - a few refinements will push it further.";
        }
        if (critical > 0) {
            return String.format("%d area%s need immediate attention to stop listings from underperforming.", critical, critical > 1 ? "s" : "");
        }
        if (needsWork > 2) {
            return "Several patterns across your listings are holding back discovery and can be improved with small changes.";
        }
        return "Overall in decent shape. The fixes below will improve consistency across your catalog.";
    }

    private List<String> buildTopFixes(List<HealthSignalDTO> signals) {
        List<String> fixes = signals.stream()
                .filter(signal -> !"strong".equals(signal.status()))
                .sorted(Comparator.comparingInt(HealthSignalDTO::score))
                .map(HealthSignalDTO::suggestion)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        signals.stream()
                .sorted(Comparator.comparingInt(HealthSignalDTO::score))
                .map(HealthSignalDTO::suggestion)
                .filter(suggestion -> !fixes.contains(suggestion))
                .forEach(fixes::add);

        int fallbackIndex = 0;
        while (fixes.size() < 3 && fallbackIndex < FALLBACK_FIXES.size()) {
            String candidate = FALLBACK_FIXES.get(fallbackIndex++);
            if (!fixes.contains(candidate)) {
                fixes.add(candidate);
            }
        }

        // Hard safety - should never reach here.
        while (fixes.size() < 3) {
            fixes.add(FALLBACK_FIXES.get(fixes.size() % FALLBACK_FIXES.size()));
        }

        return fixes.stream().limit(3).toList();
    }

    private ShopHealthDTO emptyHealth() {
        return new ShopHealthDTO(
                -1,
                "?",
                "Generate at least 5 listings to see your shop health score.",
                0,
                List.of(),
                List.of(
                        "Generate your first listing to get started.",
                        "Fill in the buyer, occasion, and shipping fields for richer analysis.",
                        "Come back after 5 listings to see stronger pattern analysis."
                ),
                OffsetDateTime.now().toInstant()
        );
    }

    private <T> T parseJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.debug("Could not parse shop health JSON as {}", type.getSimpleName(), e);
            return null;
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Could not serialize shop health snapshot", e);
            return "{}";
        }
    }
}
