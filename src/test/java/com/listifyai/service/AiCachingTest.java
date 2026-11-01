package com.listifyai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.dto.EtsyAuditDtos.AuditResultDTO;
import com.listifyai.dto.EtsyAuditDtos.CompactEtsyListing;
import com.listifyai.entity.CachedAiOutput;
import com.listifyai.entity.User;
import com.listifyai.repository.AnalyzedListingRepository;
import com.listifyai.repository.CachedAiOutputRepository;
import com.listifyai.repository.ListingAnalysisRepository;
import com.listifyai.repository.RegenerationHistoryRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked", "unused"})
public class AiCachingTest {

    @Mock private EtsyUrlParser urlParser;
    @Mock private EtsyApiClient etsyApiClient;
    @Mock private EtsyAuditClaudeService claudeService;
    @Mock private EtsyAuditValidator validator;
    @Mock private AuditUsageService auditUsageService;
    @Mock private AnalyzedListingRepository analyzedListingRepository;
    @Mock private ListingAnalysisRepository listingAnalysisRepository;
    @Mock private CachedAiOutputRepository cachedAiOutputRepository;
    @Mock private RegenerationHistoryRepository regenerationHistoryRepository;

    private EtsyListingAuditService service;
    private User testUser;
    private CompactEtsyListing testListing;

    @BeforeEach
    void setup() {
        service = new EtsyListingAuditService(
                urlParser, etsyApiClient, claudeService, validator,
                auditUsageService, analyzedListingRepository,
                listingAnalysisRepository, cachedAiOutputRepository,
                regenerationHistoryRepository, new ObjectMapper(),
                new SimpleMeterRegistry()
        );

        testUser = new User();
        testUser.setId(UUID.randomUUID());

        testListing = new CompactEtsyListing("123", "http://etsy.com/123", "Title", "Desc", null, null, null, null, null, null, null, null);
    }

    @Test
    void shouldCacheAndReturnWhenVersionsMatch() throws Exception {
        when(urlParser.parse("url")).thenReturn(new EtsyUrlParser.ParsedListingUrl("123", "http://etsy.com/123"));
        when(etsyApiClient.fetchListing("123", "http://etsy.com/123")).thenReturn(testListing);
        when(claudeService.getModel()).thenReturn("claude-3-5-sonnet");

        // Mock cache lookup miss
        when(cachedAiOutputRepository.findValidCache(
                eq("123"), eq("ETSY"), eq("full"), eq("v1.0.0"), eq("claude-3-5-sonnet"),
                eq("v1.0.0"), eq("v1.0.0"), eq("v1.0.0"), anyString(), any(OffsetDateTime.class)
        )).thenReturn(Optional.empty());

        // Mock Claude processing
        AuditResultDTO audit = new AuditResultDTO(80, "expl", "sum", null, null, null, null, null, null, null, "title", null, "desc");
        when(claudeService.audit(testListing)).thenReturn(audit);
        when(validator.validateFull(audit, testListing)).thenReturn(new EtsyAuditValidator.ValidationReport(null, null));

        // Mock saving entities
        when(cachedAiOutputRepository.save(any(CachedAiOutput.class))).thenAnswer(i -> {
            CachedAiOutput c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(analyzedListingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(listingAnalysisRepository.save(any())).thenAnswer(i -> {
            com.listifyai.entity.ListingAnalysis l = i.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        service.analyze("url", testUser);

        // Verify cache write
        ArgumentCaptor<CachedAiOutput> captor = ArgumentCaptor.forClass(CachedAiOutput.class);
        verify(cachedAiOutputRepository).save(captor.capture());
        CachedAiOutput savedCache = captor.getValue();
        assertEquals("ETSY", savedCache.getMarketplace());
        assertEquals("v1.0.0", savedCache.getPromptVersion());
        assertEquals("claude-3-5-sonnet", savedCache.getModelVersion());
        assertEquals("v1.0.0", savedCache.getValidatorVersion());
        assertEquals("v1.0.0", savedCache.getSchemaVersion());
        assertEquals("v1.0.0", savedCache.getCacheKeyVersion());
        
        // Next run - should hit cache
        when(cachedAiOutputRepository.findValidCache(
                eq("123"), eq("ETSY"), eq("full"), eq("v1.0.0"), eq("claude-3-5-sonnet"),
                eq("v1.0.0"), eq("v1.0.0"), eq("v1.0.0"), anyString(), any(OffsetDateTime.class)
        )).thenReturn(Optional.of(savedCache));

        service.analyze("url", testUser);

        // Verify Claude was only called ONCE
        verify(claudeService, times(1)).audit(any());
    }
}
