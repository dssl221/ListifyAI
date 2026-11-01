package com.listifyai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.dto.EtsyAuditDtos.*;
import com.listifyai.entity.AnalyzedListing;
import com.listifyai.entity.CachedAiOutput;
import com.listifyai.entity.ListingAnalysis;
import com.listifyai.entity.RegenerationHistory;
import com.listifyai.entity.User;
import com.listifyai.exception.ClaudeApiException;
import com.listifyai.repository.AnalyzedListingRepository;
import com.listifyai.repository.CachedAiOutputRepository;
import com.listifyai.repository.ListingAnalysisRepository;
import com.listifyai.repository.RegenerationHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;
import io.micrometer.core.instrument.MeterRegistry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class EtsyListingAuditService {

    private static final Logger log = LoggerFactory.getLogger(EtsyListingAuditService.class);
    
    private static final String FULL_SECTION = "full";
    public static final String SCHEMA_VERSION = "v1.0.0";
    public static final String CACHE_KEY_VERSION = "v1.0.0";

    private final EtsyUrlParser urlParser;
    private final EtsyApiClient etsyApiClient;
    private final EtsyAuditClaudeService claudeService;
    private final EtsyAuditValidator validator;
    private final AuditUsageService auditUsageService;
    private final AnalyzedListingRepository analyzedListingRepository;
    private final ListingAnalysisRepository listingAnalysisRepository;
    private final CachedAiOutputRepository cachedAiOutputRepository;
    private final RegenerationHistoryRepository regenerationHistoryRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public EtsyListingAuditService(EtsyUrlParser urlParser,
                                   EtsyApiClient etsyApiClient,
                                   EtsyAuditClaudeService claudeService,
                                   EtsyAuditValidator validator,
                                   AuditUsageService auditUsageService,
                                   AnalyzedListingRepository analyzedListingRepository,
                                   ListingAnalysisRepository listingAnalysisRepository,
                                   CachedAiOutputRepository cachedAiOutputRepository,
                                   RegenerationHistoryRepository regenerationHistoryRepository,
                                   ObjectMapper objectMapper,
                                   MeterRegistry meterRegistry) {
        this.urlParser = urlParser;
        this.etsyApiClient = etsyApiClient;
        this.claudeService = claudeService;
        this.validator = validator;
        this.auditUsageService = auditUsageService;
        this.analyzedListingRepository = analyzedListingRepository;
        this.listingAnalysisRepository = listingAnalysisRepository;
        this.cachedAiOutputRepository = cachedAiOutputRepository;
        this.regenerationHistoryRepository = regenerationHistoryRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public ListingAuditDTO analyze(String url, User user) {
        EtsyUrlParser.ParsedListingUrl parsed = urlParser.parse(url);
        CompactEtsyListing listing = etsyApiClient.fetchListing(parsed.listingId(), parsed.canonicalUrl());
        String inputHash = sha256(toJson(listing));

        CachedAiOutput cache = cachedAiOutputRepository
                .findValidCache(
                        listing.listingId(),
                        "ETSY",
                        FULL_SECTION,
                        EtsyAuditClaudeService.PROMPT_VERSION,
                        claudeService.getModel(),
                        EtsyAuditValidator.VALIDATOR_VERSION,
                        SCHEMA_VERSION,
                        CACHE_KEY_VERSION,
                        inputHash,
                        OffsetDateTime.now()
                )
                .orElse(null);

        boolean cacheHit = cache != null;
        if (cacheHit) {
            log.info("Cache HIT for etsyListingId={}", listing.listingId());
            meterRegistry.counter("listifyai.audit.cache", "status", "hit").increment();
        } else {
            log.info("Cache MISS for etsyListingId={}", listing.listingId());
            meterRegistry.counter("listifyai.audit.cache", "status", "miss").increment();
        }
        AuditResultDTO audit;
        AuditUsageDTO usage;
        if (cache == null) {
            usage = auditUsageService.checkAndIncrement(user);
            audit = createValidatedAudit(listing);
            cache = saveCache(listing, inputHash, audit);
        } else {
            usage = auditUsageService.getUsage(user);
            audit = fromJson(cache.getOutputJson(), AuditResultDTO.class);
        }

        AnalyzedListing analyzedListing = new AnalyzedListing();
        analyzedListing.setUser(user);
        analyzedListing.setEtsyListingId(listing.listingId());
        analyzedListing.setListingUrl(listing.url());
        analyzedListing.setTitle(listing.title());
        analyzedListing.setListingSnapshot(toJson(listing));
        analyzedListing.setCacheHit(cacheHit);
        analyzedListing = analyzedListingRepository.save(analyzedListing);

        EtsyAuditValidator.ValidationReport validation = validator.validateFull(audit, listing);
        ListingAnalysis analysis = new ListingAnalysis();
        analysis.setAnalyzedListing(analyzedListing);
        analysis.setUser(user);
        analysis.setScore(audit.score());
        analysis.setAuditJson(toJson(audit));
        analysis.setValidationJson(toJson(validation));
        analysis.setPromptVersion(EtsyAuditClaudeService.PROMPT_VERSION);
        analysis.setCachedOutputId(cache.getId());
        analysis = listingAnalysisRepository.save(analysis);

        return new ListingAuditDTO(
                analysis.getId(),
                cacheHit,
                toListingDto(analyzedListing, listing),
                usage,
                audit
        );
    }

    @Transactional
    public SectionRegenerationDTO regenerate(UUID analysisId, String section, User user) {
        ListingAnalysis analysis = getOwnedAnalysis(analysisId, user);
        CompactEtsyListing listing = fromJson(analysis.getAnalyzedListing().getListingSnapshot(), CompactEtsyListing.class);
        AuditResultDTO current = fromJson(analysis.getAuditJson(), AuditResultDTO.class);
        JsonNode regenerated = claudeService.regenerateSection(section, listing, current);
        AuditResultDTO updated = mergeSection(section, current, regenerated, listing);
        EtsyAuditValidator.ValidationReport validation = validator.validateFull(updated, listing);

        Object previous = sectionValue(section, current);
        Object next = sectionValue(section, updated);
        analysis.setAuditJson(toJson(updated));
        analysis.setValidationJson(toJson(validation));
        analysis.setScore(updated.score());
        listingAnalysisRepository.save(analysis);
        saveRegeneration(analysis, user, section, previous, next, "seller_requested");

        return new SectionRegenerationDTO(analysis.getId(), section, next, updated);
    }

    @Transactional(readOnly = true)
    public Page<HistoryItemDTO> history(User user, int page, int size) {
        return listingAnalysisRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(analysis -> new HistoryItemDTO(
                        analysis.getId(),
                        analysis.getAnalyzedListing().getEtsyListingId(),
                        analysis.getAnalyzedListing().getListingUrl(),
                        analysis.getAnalyzedListing().getTitle(),
                        analysis.getScore(),
                        analysis.getCreatedAt().toInstant()
                ));
    }

    private AuditResultDTO createValidatedAudit(CompactEtsyListing listing) {
        AuditResultDTO audit = claudeService.audit(listing);
        EtsyAuditValidator.ValidationReport report = validator.validateFull(audit, listing);
        if (report.passed()) {
            return audit;
        }

        AuditResultDTO updated = audit;
        for (String failedSection : report.failedSections()) {
            if ("title".equals(failedSection)) {
                updated = mergeSection("title", updated, claudeService.regenerateSection("title", listing, updated), listing);
            } else if ("tags".equals(failedSection)) {
                updated = mergeSection("tags", updated, claudeService.regenerateSection("tags", listing, updated), listing);
            } else if ("description".equals(failedSection)) {
                updated = mergeSection("description", updated, claudeService.regenerateSection("description", listing, updated), listing);
            }
        }
        EtsyAuditValidator.ValidationReport finalReport = validator.validateFull(updated, listing);
        if (!finalReport.passed()) {
            throw new ClaudeApiException("Claude returned audit content that did not pass Tagwing validation.");
        }
        return updated;
    }

    private AuditResultDTO mergeSection(String section, AuditResultDTO current, JsonNode regenerated, CompactEtsyListing listing) {
        String title = current.optimizedTitle();
        List<String> tags = current.optimizedTags();
        String description = current.optimizedDescription();

        if ("title".equals(section)) {
            title = regenerated.path("optimizedTitle").asText();
            if (!validator.isValidTitle(title, listing)) {
                throw new ClaudeApiException("Claude returned a title that did not pass Tagwing validation.");
            }
        } else if ("tags".equals(section)) {
            try {
                tags = objectMapper.readerForListOf(String.class).readValue(regenerated.path("optimizedTags"));
            } catch (Exception ex) {
                throw new ClaudeApiException("Claude returned tags that could not be parsed.", ex);
            }
            if (!validator.isValidTags(tags)) {
                throw new ClaudeApiException("Claude returned tags that did not pass Tagwing validation.");
            }
        } else if ("description".equals(section)) {
            description = regenerated.path("optimizedDescription").asText();
            if (!validator.isValidDescription(description)) {
                throw new ClaudeApiException("Claude returned a description that did not pass Tagwing validation.");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported regeneration section.");
        }

        return new AuditResultDTO(
                current.score(),
                current.scoreExplanation(),
                current.summary(),
                current.weakTagAnalysis(),
                current.titleClarityAnalysis(),
                current.buyerIntentAnalysis(),
                current.personalizationAnalysis(),
                current.giftingIntentAnalysis(),
                current.readabilityReview(),
                current.actionableFixes(),
                title,
                tags,
                description
        );
    }

    private ListingAnalysis getOwnedAnalysis(UUID analysisId, User user) {
        ListingAnalysis analysis = listingAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit not found."));
        if (!analysis.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to edit this audit.");
        }
        return analysis;
    }

    private CachedAiOutput saveCache(CompactEtsyListing listing, String inputHash, AuditResultDTO audit) {
        CachedAiOutput output = new CachedAiOutput();
        output.setEtsyListingId(listing.listingId());
        output.setMarketplace("ETSY");
        output.setSection(FULL_SECTION);
        output.setPromptVersion(EtsyAuditClaudeService.PROMPT_VERSION);
        output.setModelVersion(claudeService.getModel());
        output.setValidatorVersion(EtsyAuditValidator.VALIDATOR_VERSION);
        output.setSchemaVersion(SCHEMA_VERSION);
        output.setCacheKeyVersion(CACHE_KEY_VERSION);
        output.setInputHash(inputHash);
        output.setOutputJson(toJson(audit));
        output.setExpiresAt(OffsetDateTime.now().plusDays(7));
        return cachedAiOutputRepository.save(output);
    }

    private void saveRegeneration(ListingAnalysis analysis, User user, String section, Object previous, Object next, String reason) {
        RegenerationHistory history = new RegenerationHistory();
        history.setAnalysis(analysis);
        history.setUser(user);
        history.setSection(section);
        history.setPreviousValue(toJson(previous));
        history.setNewValue(toJson(next));
        history.setReason(reason);
        regenerationHistoryRepository.save(history);
    }

    private Object sectionValue(String section, AuditResultDTO audit) {
        return switch (section) {
            case "title" -> audit.optimizedTitle();
            case "tags" -> audit.optimizedTags();
            case "description" -> audit.optimizedDescription();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported regeneration section.");
        };
    }

    private AnalyzedListingDTO toListingDto(AnalyzedListing entity, CompactEtsyListing listing) {
        return new AnalyzedListingDTO(
                entity.getId(),
                entity.getEtsyListingId(),
                entity.getListingUrl(),
                listing.title(),
                listing.tags(),
                listing.description(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toInstant()
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ClaudeApiException("JSON serialization failed: " + ex.getMessage(), ex);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new ClaudeApiException("JSON parsing failed: " + ex.getMessage(), ex);
        }
    }
}
