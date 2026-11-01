package com.listifyai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.dto.EtsyAuditDtos.AuditResultDTO;
import com.listifyai.dto.EtsyAuditDtos.CompactEtsyListing;
import com.listifyai.entity.User;
import com.listifyai.repository.CachedAiOutputRepository;
import com.listifyai.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class AiCachingIntegrationTest {

    @Autowired
    private EtsyListingAuditService auditService;

    @Autowired
    private CachedAiOutputRepository cachedAiOutputRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.listifyai.repository.AnalyzedListingRepository analyzedListingRepository;

    @Autowired
    private com.listifyai.repository.ListingAnalysisRepository listingAnalysisRepository;

    @MockBean
    private EtsyApiClient etsyApiClient;

    @MockBean
    private EtsyAuditClaudeService claudeService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private CompactEtsyListing mockListing;
    private AuditResultDTO validAudit;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("cache-test@example.com");
        testUser.setPlan("pro");
        testUser.setSupabaseId(UUID.randomUUID().toString());
        testUser = userRepository.save(testUser);

        mockListing = new CompactEtsyListing("123", "https://etsy.com/listing/123", "Title", "Desc", List.of("tag1"), "path", "who", "when", "yes", "100", "USD", java.util.Map.of());
        when(etsyApiClient.fetchListing(any(), any())).thenReturn(mockListing);

        validAudit = new AuditResultDTO(
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
        when(claudeService.getModel()).thenReturn("claude-3-haiku-20240307");
    }

    @AfterEach
    void tearDown() {
        cachedAiOutputRepository.deleteAll();
        listingAnalysisRepository.deleteAll();
        analyzedListingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCacheHitReturnsStoredOutputAndAvoidsClaude() {
        when(claudeService.audit(any())).thenReturn(validAudit);

        // First call - should trigger Claude
        var firstResult = auditService.analyze("https://etsy.com/listing/123", testUser);
        assertNotNull(firstResult);
        verify(claudeService, times(1)).audit(any());
        assertEquals(false, firstResult.cached());

        // Second call - should hit cache
        var secondResult = auditService.analyze("https://etsy.com/listing/123", testUser);
        assertNotNull(secondResult);
        
        // claudeService.audit should NOT be called again
        verify(claudeService, times(1)).audit(any());
        assertEquals(true, secondResult.cached());
        assertEquals(100, secondResult.audit().score());
    }

    @Test
    void testExpiredCacheNotReturned() {
        when(claudeService.audit(any())).thenReturn(validAudit);

        // First call triggers Claude and saves cache
        auditService.analyze("https://etsy.com/listing/123", testUser);
        verify(claudeService, times(1)).audit(any());

        // Expire the cache manually
        var caches = cachedAiOutputRepository.findAll();
        for (var cache : caches) {
            cache.setExpiresAt(java.time.OffsetDateTime.now().minusDays(1));
            cachedAiOutputRepository.save(cache);
        }

        // Second call should NOT hit cache because it is expired
        var result = auditService.analyze("https://etsy.com/listing/123", testUser);
        assertEquals(false, result.cached());
        verify(claudeService, times(2)).audit(any());
    }

    @Test
    void testChangedPromptVersionInvalidatesCache() {
        when(claudeService.audit(any())).thenReturn(validAudit);

        // First call triggers Claude and saves cache
        auditService.analyze("https://etsy.com/listing/123", testUser);
        verify(claudeService, times(1)).audit(any());

        // Change prompt version on the cache record manually to simulate old version
        var caches = cachedAiOutputRepository.findAll();
        for (var cache : caches) {
            cache.setPromptVersion("vOld.0.0");
            cachedAiOutputRepository.save(cache);
        }

        // Second call should NOT hit cache because version mismatch
        var result = auditService.analyze("https://etsy.com/listing/123", testUser);
        assertEquals(false, result.cached());
        verify(claudeService, times(2)).audit(any());
    }
}
