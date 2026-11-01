package com.listifyai.monitoring;

import com.listifyai.dto.EtsyAuditDtos.CompactEtsyListing;
import com.listifyai.dto.MonitoringDtos.*;
import com.listifyai.entity.*;
import com.listifyai.monitoring.MonitoringEnums.*;
import com.listifyai.monitoring.rules.MonitoringRuleContext;
import com.listifyai.monitoring.rules.MonitoringRuleEngine;
import com.listifyai.monitoring.rules.MonitoringRuleFinding;
import com.listifyai.repository.*;
import com.listifyai.service.EtsyApiClient;
import com.listifyai.service.EtsyUrlParser;
import com.listifyai.service.AuditService;
import com.listifyai.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class ListingMonitoringService {

    private final MonitoredListingRepository monitoredListingRepository;
    private final ListingSnapshotRepository snapshotRepository;
    private final MonitoringRecommendationRepository recommendationRepository;
    private final MonitoringEventRepository eventRepository;
    private final MonitoringJobRepository jobRepository;
    private final EtsyUrlParser urlParser;
    private final EtsyApiClient etsyApiClient;
    private final MonitoringQueueService queueService;
    private final MonitoringPlanService planService;
    private final ListingScoreService scoreService;
    private final MonitoringChecksumService checksumService;
    private final MonitoringRuleEngine ruleEngine;
    private final MonitoringJson json;
    private final MonitoringProperties properties;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public ListingMonitoringService(MonitoredListingRepository monitoredListingRepository,
                                    ListingSnapshotRepository snapshotRepository,
                                    MonitoringRecommendationRepository recommendationRepository,
                                    MonitoringEventRepository eventRepository,
                                    MonitoringJobRepository jobRepository,
                                    EtsyUrlParser urlParser,
                                    EtsyApiClient etsyApiClient,
                                    MonitoringQueueService queueService,
                                    MonitoringPlanService planService,
                                    ListingScoreService scoreService,
                                    MonitoringChecksumService checksumService,
                                    MonitoringRuleEngine ruleEngine,
                                    MonitoringJson json,
                                    MonitoringProperties properties,
                                    NotificationService notificationService,
                                    AuditService auditService) {
        this.monitoredListingRepository = monitoredListingRepository;
        this.snapshotRepository = snapshotRepository;
        this.recommendationRepository = recommendationRepository;
        this.eventRepository = eventRepository;
        this.jobRepository = jobRepository;
        this.urlParser = urlParser;
        this.etsyApiClient = etsyApiClient;
        this.queueService = queueService;
        this.planService = planService;
        this.scoreService = scoreService;
        this.checksumService = checksumService;
        this.ruleEngine = ruleEngine;
        this.json = json;
        this.properties = properties;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public MonitoredListingDTO enable(String url, User user) {
        EtsyUrlParser.ParsedListingUrl parsed = urlParser.parse(url);
        MonitoredListing listing = monitoredListingRepository
                .findByUserIdAndEtsyListingId(user.getId(), parsed.listingId())
                .orElseGet(MonitoredListing::new);
        listing.setUser(user);
        listing.setEtsyListingId(parsed.listingId());
        listing.setListingUrl(parsed.canonicalUrl());
        listing.setStatus(MonitoredListingStatus.ACTIVE);
        listing.setMonitoringEnabled(true);
        listing.setNextCheckAt(OffsetDateTime.now());
        listing = monitoredListingRepository.save(listing);
        MonitoringJob job = queueService.enqueueListingCheck(listing, "user_enabled", planService.priority(user));
        listing.setLastJobId(job.getId());
        monitoredListingRepository.save(listing);
        recordEvent(user, listing, job, "monitoring.enabled", MonitoringSeverity.INFO, "Monitoring enabled for listing.", null);
        auditService.log(user, "monitoring.enabled", "MonitoredListing", listing.getId().toString(), null, Map.of("enabled", true));
        return toListingDto(listing);
    }

    @Transactional
    public MonitoredListingDTO disable(UUID listingId, User user) {
        MonitoredListing listing = getOwnedListing(listingId, user);
        listing.setMonitoringEnabled(false);
        listing.setStatus(MonitoredListingStatus.PAUSED);
        recordEvent(user, listing, null, "monitoring.disabled", MonitoringSeverity.INFO, "Monitoring disabled for listing.", null);
        auditService.log(user, "monitoring.disabled", "MonitoredListing", listing.getId().toString(), Map.of("enabled", true), Map.of("enabled", false));
        return toListingDto(monitoredListingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public long countMonitoredListings(User user) {
        return monitoredListingRepository.countByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public List<MonitoredListingDTO> listings(User user) {
        return monitoredListingRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toListingDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingSnapshotDTO> history(UUID listingId, User user) {
        MonitoredListing listing = getOwnedListing(listingId, user);
        return snapshotRepository
                .findByMonitoredListingIdAndUserIdOrderByCapturedAtDesc(listing.getId(), user.getId(), PageRequest.of(0, 100))
                .stream()
                .map(this::toSnapshotDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonitoringRecommendationDTO> recommendations(UUID listingId, User user) {
        MonitoredListing listing = getOwnedListing(listingId, user);
        return recommendationRepository
                .findByMonitoredListingIdAndUserIdAndStatusInOrderByDetectedAtDesc(
                        listing.getId(),
                        user.getId(),
                        List.of(MonitoringRecommendationStatus.OPEN, MonitoringRecommendationStatus.PENDING_AI),
                        PageRequest.of(0, 100)
                )
                .stream()
                .map(this::toRecommendationDto)
                .toList();
    }

    public CompactEtsyListing fetchLiveListing(MonitoredListing listing) {
        return etsyApiClient.fetchListing(listing.getEtsyListingId(), listing.getListingUrl());
    }

    @Transactional
    public void persistMonitoringResult(UUID listingId, UUID jobId, CompactEtsyListing liveListing) {
        MonitoredListing listing = monitoredListingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitored listing not found."));
        MonitoringJob job = jobId == null ? null : jobRepository.findById(jobId).orElse(null);
        ListingSnapshot previous = snapshotRepository.findFirstByMonitoredListingIdOrderByCapturedAtDesc(listingId).orElse(null);
        OffsetDateTime now = OffsetDateTime.now();
        ListingSnapshot snapshot = buildSnapshot(listing, liveListing, now);
        snapshot = snapshotRepository.save(snapshot);

        List<String> tags = liveListing.tags() == null ? List.of() : liveListing.tags();
        MonitoringRuleContext context = new MonitoringRuleContext(listing, snapshot, previous, tags, now);
        List<MonitoringRuleFinding> findings = ruleEngine.evaluate(context);
        for (MonitoringRuleFinding finding : findings) {
            saveRecommendation(listing, snapshot, finding);
            recordEvent(
                    listing.getUser(),
                    listing,
                    job,
                    "monitoring.finding." + finding.type().toLowerCase(Locale.ROOT),
                    finding.severity(),
                    finding.message(),
                    finding.details(),
                    finding.ruleVersion(),
                    finding.rulesEngineVersion()
            );
        }

        recordEvent(
                listing.getUser(),
                listing,
                job,
                "monitoring.completed",
                MonitoringSeverity.INFO,
                "Monitoring check completed.",
                Map.of("findings", findings.size(), "score", snapshot.getScore())
        );

        listing.setTitle(liveListing.title());
        listing.setLastSnapshot(snapshot);
        listing.setLastSnapshotAt(snapshot.getCapturedAt());
        listing.setLastScore(snapshot.getScore());
        listing.setLastCheckedAt(now);
        listing.setNextCheckAt(planService.nextCheckAt(listing.getUser(), now));
        monitoredListingRepository.save(listing);
    }

    private ListingSnapshot buildSnapshot(MonitoredListing listing, CompactEtsyListing liveListing, OffsetDateTime now) {
        int score = scoreService.score(liveListing);
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("title", liveListing.title());
        normalized.put("description", liveListing.description());
        normalized.put("tags", liveListing.tags());
        normalized.put("price", liveListing.price());
        normalized.put("currency", liveListing.currency());
        normalized.put("score", score);
        String checksum = checksumService.sha256(json.write(normalized));

        ListingSnapshot snapshot = new ListingSnapshot();
        snapshot.setMonitoredListing(listing);
        snapshot.setUser(listing.getUser());
        snapshot.setEtsyListingId(listing.getEtsyListingId());
        snapshot.setTitle(liveListing.title());
        snapshot.setDescription(liveListing.description());
        snapshot.setTags(json.write(liveListing.tags() == null ? List.of() : liveListing.tags()));
        snapshot.setPrice(liveListing.price());
        snapshot.setCurrency(liveListing.currency());
        snapshot.setViews(intSignal(liveListing, "views"));
        snapshot.setFavorites(intSignal(liveListing, "numFavorers"));
        snapshot.setScore(score);
        snapshot.setOptimizationFlags(json.write(Map.of("aiTriggered", false, "phase", "rule_based_foundation")));
        snapshot.setChecksum(checksum);
        String sourceSnapshot = json.write(liveListing);
        if (sourceSnapshot.length() > 32768) {
            sourceSnapshot = json.write(Map.of(
                    "truncated", true,
                    "title", liveListing.title(),
                    "tags", liveListing.tags() == null ? List.of() : liveListing.tags(),
                    "price", liveListing.price()
            ));
        }
        snapshot.setSourceSnapshot(sourceSnapshot);
        snapshot.setCapturedAt(now);
        return snapshot;
    }

    private void saveRecommendation(MonitoredListing listing, ListingSnapshot snapshot, MonitoringRuleFinding finding) {
        boolean aiCandidate = finding.opportunityScore() >= properties.getAiCandidateThreshold();
        String dedupeKey = checksumService.sha256(listing.getId() + ":" + finding.type() + ":" + snapshot.getChecksum());
        if (recommendationRepository.findByDedupeKey(dedupeKey).isPresent()) {
            return;
        }
        MonitoringRecommendation recommendation = new MonitoringRecommendation();
        recommendation.setUser(listing.getUser());
        recommendation.setMonitoredListing(listing);
        recommendation.setSnapshot(snapshot);
        recommendation.setRecommendationType(finding.type());
        recommendation.setStatus(aiCandidate ? MonitoringRecommendationStatus.PENDING_AI : MonitoringRecommendationStatus.OPEN);
        recommendation.setSeverity(finding.severity());
        recommendation.setOpportunityScore(finding.opportunityScore());
        recommendation.setTitle(finding.title());
        recommendation.setMessage(finding.message());
        recommendation.setDetails(json.write(finding.details()));
        recommendation.setAiCandidate(aiCandidate);
        recommendation.setDedupeKey(dedupeKey);
        recommendation.setDetectedAt(OffsetDateTime.now());
        recommendationRepository.save(recommendation);
    }

    private void recordEvent(User user,
                             MonitoredListing listing,
                             MonitoringJob job,
                             String type,
                             MonitoringSeverity severity,
                             String message,
                             Map<String, Object> details) {
        recordEvent(user, listing, job, type, severity, message, details, "1.0", "1.0");
    }

    private void recordEvent(User user,
                             MonitoredListing listing,
                             MonitoringJob job,
                             String type,
                             MonitoringSeverity severity,
                             String message,
                             Map<String, Object> details,
                             String ruleVersion,
                             String rulesEngineVersion) {
        MonitoringEvent event = new MonitoringEvent();
        event.setUser(user);
        event.setMonitoredListing(listing);
        event.setMonitoringJob(job);
        event.setEventType(type);
        event.setSeverity(severity);
        event.setMessage(message);
        event.setDetails(details == null ? null : json.write(details));
        event.setRuleVersion(ruleVersion);
        event.setRulesEngineVersion(rulesEngineVersion);
        event = eventRepository.save(event);
        if (severity == MonitoringSeverity.HIGH || severity == MonitoringSeverity.CRITICAL) {
            try {
                notificationService.sendMonitoringAlert(user, event);
            } catch (RuntimeException ignored) {
                // Monitoring events must be durable even if email delivery is temporarily unavailable.
            }
        }
    }

    private MonitoredListing getOwnedListing(UUID listingId, User user) {
        return monitoredListingRepository.findByIdAndUserId(listingId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitored listing not found."));
    }

    private MonitoredListingDTO toListingDto(MonitoredListing listing) {
        long openCount = recommendationRepository.countByMonitoredListingIdAndStatusIn(
                listing.getId(),
                List.of(MonitoringRecommendationStatus.OPEN, MonitoringRecommendationStatus.PENDING_AI)
        );
        return new MonitoredListingDTO(
                listing.getId(),
                listing.getEtsyListingId(),
                listing.getListingUrl(),
                listing.getTitle(),
                listing.getStatus().name(),
                Boolean.TRUE.equals(listing.getMonitoringEnabled()),
                listing.getLastScore(),
                listing.getLastCheckedAt() == null ? null : listing.getLastCheckedAt().toInstant(),
                listing.getNextCheckAt() == null ? null : listing.getNextCheckAt().toInstant(),
                listing.getOptimizationState().name(),
                openCount
        );
    }

    private ListingSnapshotDTO toSnapshotDto(ListingSnapshot snapshot) {
        return new ListingSnapshotDTO(
                snapshot.getId(),
                snapshot.getMonitoredListing().getId(),
                snapshot.getEtsyListingId(),
                snapshot.getTitle(),
                readTags(snapshot.getTags()),
                snapshot.getPrice(),
                snapshot.getCurrency(),
                snapshot.getViews(),
                snapshot.getFavorites(),
                snapshot.getScore(),
                snapshot.getChecksum(),
                snapshot.getCapturedAt().toInstant()
        );
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

    private int intSignal(CompactEtsyListing listing, String key) {
        Object value = listing.signals() == null ? null : listing.signals().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private List<String> readTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        return json.read(tagsJson, List.class);
    }
}
