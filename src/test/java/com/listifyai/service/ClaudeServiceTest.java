package com.listifyai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.dto.EtsyAuditDtos.AuditResultDTO;
import com.listifyai.dto.EtsyAuditDtos.CompactEtsyListing;
import com.listifyai.entity.User;
import com.listifyai.exception.ClaudeApiException;
import com.listifyai.repository.AnalyzedListingRepository;
import com.listifyai.repository.CachedAiOutputRepository;
import com.listifyai.repository.ListingAnalysisRepository;
import com.listifyai.repository.RegenerationHistoryRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaudeServiceTest {

    @Mock private EtsyUrlParser urlParser;
    @Mock private EtsyApiClient etsyApiClient;
    @Mock private EtsyAuditClaudeService claudeService;
    @Mock private AuditUsageService auditUsageService;
    @Mock private AnalyzedListingRepository analyzedListingRepository;
    @Mock private ListingAnalysisRepository listingAnalysisRepository;
    @Mock private CachedAiOutputRepository cachedAiOutputRepository;
    @Mock private RegenerationHistoryRepository regenerationHistoryRepository;

    private EtsyAuditValidator validator = new EtsyAuditValidator();
    private ObjectMapper objectMapper = new ObjectMapper();

    private EtsyListingAuditService auditService;
    private User testUser;
    private CompactEtsyListing mockListing;

    @BeforeEach
    void setUp() {
        auditService = new EtsyListingAuditService(
                urlParser, etsyApiClient, claudeService, validator,
                auditUsageService, analyzedListingRepository,
                listingAnalysisRepository, cachedAiOutputRepository,
                regenerationHistoryRepository, objectMapper,
                new SimpleMeterRegistry()
        );

        testUser = new User();
        testUser.setId(UUID.randomUUID());

        mockListing = new CompactEtsyListing("123", "https://etsy.com/123", "Title", "Desc", List.of("tag1"), "path", "who", "when", "yes", "100", "USD", java.util.Map.of());

        EtsyUrlParser.ParsedListingUrl parsed = new EtsyUrlParser.ParsedListingUrl("123", "https://etsy.com/123");
        lenient().when(urlParser.parse("https://etsy.com/123")).thenReturn(parsed);
        lenient().when(etsyApiClient.fetchListing("123", "https://etsy.com/123")).thenReturn(mockListing);
        lenient().when(cachedAiOutputRepository.findValidCache(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(cachedAiOutputRepository.save(any())).thenAnswer(inv -> {
            com.listifyai.entity.CachedAiOutput entity = inv.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });
        lenient().when(analyzedListingRepository.save(any())).thenAnswer(inv -> {
            com.listifyai.entity.AnalyzedListing entity = inv.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });
        lenient().when(listingAnalysisRepository.save(any())).thenAnswer(inv -> {
            com.listifyai.entity.ListingAnalysis entity = inv.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });
    }

    @Test
    void testValidStructuredOutputAccepted() {
        AuditResultDTO validAudit = new AuditResultDTO(
                100, "Good title quality and tag quality because they are awesome", "Summary without any bad words", 
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                List.of("Fix 1", "Fix 2", "Fix 3"),
                "Optimized Title That Is Very Good For SEO Because It Is Very Descriptive and Also Valid",
                List.of("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tag10", "tag11", "tag12", "tag13"),
                "This description is extremely long and valid. ".repeat(15)
        );
        when(claudeService.audit(mockListing)).thenReturn(validAudit);

        assertDoesNotThrow(() -> auditService.analyze("https://etsy.com/123", testUser));
    }

    @Test
    void testInvalidJsonRejected() {
        when(claudeService.audit(mockListing)).thenThrow(new ClaudeApiException("Failed to parse JSON"));

        Exception e = assertThrows(ClaudeApiException.class, () -> auditService.analyze("https://etsy.com/123", testUser));
        assertEquals("Failed to parse JSON", e.getMessage());
    }

    @Test
    void testWrongTagCountRejected() {
        // Validation fails and triggers a regenerate section call that also fails
        AuditResultDTO invalidTagsAudit = new AuditResultDTO(
                100, "Good title quality and tag quality because they are awesome", "Summary without any bad words", 
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                List.of("Fix 1", "Fix 2", "Fix 3"),
                "Optimized Title That Is Very Good For SEO Because It Is Very Descriptive and Also Valid",
                List.of("only", "two", "tags"), // Invalid: must be 13 tags
                "This description is extremely long and valid. ".repeat(15)
        );
        when(claudeService.audit(mockListing)).thenReturn(invalidTagsAudit);
        
        // Mock regenerate to also return invalid
        com.fasterxml.jackson.databind.node.ObjectNode regeneratedTags = objectMapper.createObjectNode();
        regeneratedTags.putArray("optimizedTags").add("only").add("two");
        when(claudeService.regenerateSection(eq("tags"), any(), any()))
                .thenReturn(regeneratedTags);

        Exception e = assertThrows(ClaudeApiException.class, () -> auditService.analyze("https://etsy.com/123", testUser));
        assertTrue(e.getMessage().contains("Claude returned audit content that did not pass Tagwing validation") || 
                   e.getMessage().contains("Claude returned tags that did not pass Tagwing validation"));
    }

    @Test
    void testWrongTitleCountRejected() {
        AuditResultDTO invalidTitleAudit = new AuditResultDTO(
                100, "Good title quality and tag quality because they are awesome", "Summary without any bad words", 
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                new com.listifyai.dto.EtsyAuditDtos.SectionInsightDTO("GOOD", "msg", "fix"),
                List.of("Fix 1", "Fix 2", "Fix 3"),
                "Too short", // Invalid title length
                List.of("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tag10", "tag11", "tag12", "tag13"),
                "This description is extremely long and valid. ".repeat(15)
        );
        when(claudeService.audit(mockListing)).thenReturn(invalidTitleAudit);
        
        // Mock regenerate to also return invalid
        when(claudeService.regenerateSection(eq("title"), any(), any()))
                .thenReturn(objectMapper.createObjectNode().put("optimizedTitle", "Still short"));

        Exception e = assertThrows(ClaudeApiException.class, () -> auditService.analyze("https://etsy.com/123", testUser));
        assertTrue(e.getMessage().contains("Claude returned audit content that did not pass Tagwing validation") || 
                   e.getMessage().contains("Claude returned a title that did not pass Tagwing validation"));
    }

    @Test
    void testTimeoutHandled() {
        when(claudeService.audit(mockListing)).thenThrow(new ClaudeApiException("Request timeout to Claude"));

        Exception e = assertThrows(ClaudeApiException.class, () -> auditService.analyze("https://etsy.com/123", testUser));
        assertEquals("Request timeout to Claude", e.getMessage());
    }

    @Test
    void testRateLimitHandled() {
        when(claudeService.audit(mockListing)).thenThrow(new ClaudeApiException("Rate limit exceeded"));

        Exception e = assertThrows(ClaudeApiException.class, () -> auditService.analyze("https://etsy.com/123", testUser));
        assertEquals("Rate limit exceeded", e.getMessage());
    }
}
