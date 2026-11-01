package com.listifyai.service;

import com.listifyai.dto.EtsyAuditDtos.AuditResultDTO;
import com.listifyai.dto.EtsyAuditDtos.CompactEtsyListing;
import com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class EtsyAuditValidator {
    
    public static final String VALIDATOR_VERSION = "v1.0.0";

    private static final Set<String> GENERIC_TAGS = Set.of(
            "gift", "gifts", "handmade", "etsy", "sale", "present", "custom", "personalized",
            "birthday", "christmas", "jewelry", "home decor", "shirt", "print"
    );
    private static final Set<String> FORBIDDEN_JARGON = Set.of(
            "keyword saturation", "semantic indexing", "ctr optimization", "keyword density",
            "metadata insufficiency", "lexical diversity"
    );
    private static final Set<String> SCORE_FACTOR_NAMES = Set.of(
            "title quality", "tag quality", "buyer clarity", "search specificity",
            "readability", "gifting intent", "personalization signals"
    );

    public ValidationReport validateFull(AuditResultDTO audit, CompactEtsyListing listing) {
        List<String> failed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (audit == null) {
            return new ValidationReport(List.of("full"), List.of("Claude returned no audit."));
        }

        if (audit.score() < 0 || audit.score() > 100) {
            failed.add("score");
        }
        if (blank(audit.summary()) || containsJargon(audit.summary())) {
            failed.add("summary");
        }
        if (blank(audit.scoreExplanation())
                || containsJargon(audit.scoreExplanation())
                || mentionedScoreFactors(audit.scoreExplanation()) < 2) {
            failed.add("summary");
        }
        if (audit.actionableFixes() == null || audit.actionableFixes().size() != 3) {
            failed.add("fixes");
        }
        validateTitle(audit.optimizedTitle(), listing, failed, warnings);
        validateTags(audit.optimizedTags(), failed, warnings);
        validateDescription(audit.optimizedDescription(), failed, warnings);
        validateInsight(audit.weakTagAnalysis(), "weakTagAnalysis", failed);
        validateInsight(audit.titleClarityAnalysis(), "titleClarityAnalysis", failed);
        validateInsight(audit.buyerIntentAnalysis(), "buyerIntentAnalysis", failed);
        validateInsight(audit.personalizationAnalysis(), "personalizationAnalysis", failed);
        validateInsight(audit.giftingIntentAnalysis(), "giftingIntentAnalysis", failed);
        validateInsight(audit.readabilityReview(), "readabilityReview", failed);

        return new ValidationReport(failed.stream().distinct().toList(), warnings);
    }

    public boolean isValidTitle(String title, CompactEtsyListing listing) {
        List<String> failed = new ArrayList<>();
        validateTitle(title, listing, failed, new ArrayList<>());
        return failed.isEmpty();
    }

    public boolean isValidTags(List<String> tags) {
        List<String> failed = new ArrayList<>();
        validateTags(tags, failed, new ArrayList<>());
        return failed.isEmpty();
    }

    public boolean isValidDescription(String description) {
        List<String> failed = new ArrayList<>();
        validateDescription(description, failed, new ArrayList<>());
        return failed.isEmpty();
    }

    private void validateTitle(String title, CompactEtsyListing listing, List<String> failed, List<String> warnings) {
        if (blank(title) || title.length() > 140 || title.length() < 45 || hasRepeatedWords(title)) {
            failed.add("title");
            return;
        }
        if (!title.contains(",") && !title.contains("|")) {
            warnings.add("Title would scan better with clear phrase breaks.");
        }
        if (!sharesMeaningfulWord(title, listing.title())) {
            failed.add("title");
        }
    }

    private void validateTags(List<String> tags, List<String> failed, List<String> warnings) {
        if (tags == null || tags.size() != 13) {
            failed.add("tags");
            return;
        }
        Set<String> seen = new HashSet<>();
        int genericCount = 0;
        for (String tag : tags) {
            String normalized = normalize(tag);
            if (blank(tag) || tag.length() > 20 || !seen.add(normalized)) {
                failed.add("tags");
            }
            if (GENERIC_TAGS.contains(normalized)) {
                genericCount++;
            }
        }
        if (genericCount > 2) {
            failed.add("tags");
        } else if (genericCount > 0) {
            warnings.add("Some tags are broad; use them only when balanced with specific phrases.");
        }
    }

    private void validateDescription(String description, List<String> failed, List<String> warnings) {
        if (blank(description)) {
            return;
        }
        if (description.length() < 250 || description.length() > 1400 || containsJargon(description) || hasRepeatedWords(description)) {
            failed.add("description");
        }
        if (!description.toLowerCase(Locale.ROOT).contains("gift")) {
            warnings.add("Description could call out gifting more clearly.");
        }
    }

    private void validateInsight(SectionInsightDTO insight, String section, List<String> failed) {
        if (insight == null
                || blank(insight.status())
                || blank(insight.message())
                || blank(insight.fix())
                || containsJargon(insight.message())
                || containsJargon(insight.fix())) {
            failed.add(section);
        }
    }

    private boolean sharesMeaningfulWord(String value, String original) {
        Set<String> originalWords = meaningfulWords(original);
        for (String word : meaningfulWords(value)) {
            if (originalWords.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> meaningfulWords(String value) {
        Set<String> words = new HashSet<>();
        if (value == null) {
            return words;
        }
        for (String word : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {
            if (word.length() > 3 && !Set.of("with", "from", "that", "this", "your", "gift").contains(word)) {
                words.add(word);
            }
        }
        return words;
    }

    private boolean hasRepeatedWords(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+");
        int streak = 1;
        String previous = "";
        for (String word : words) {
            if (word.length() < 4) {
                continue;
            }
            if (word.equals(previous)) {
                streak++;
                if (streak >= 3) {
                    return true;
                }
            } else {
                streak = 1;
                previous = word;
            }
        }
        return false;
    }

    private boolean containsJargon(String value) {
        String normalized = normalize(value);
        return FORBIDDEN_JARGON.stream().anyMatch(normalized::contains);
    }

    private long mentionedScoreFactors(String value) {
        String normalized = normalize(value);
        return SCORE_FACTOR_NAMES.stream().filter(normalized::contains).count();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    public record ValidationReport(List<String> failedSections, List<String> warnings) {
        public boolean passed() {
            return failedSections == null || failedSections.isEmpty();
        }
    }
}
