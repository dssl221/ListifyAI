package com.listifyai.service;

import com.listifyai.dto.ProductInputDTO;
import com.listifyai.entity.ClaudeFailureReason;
import com.listifyai.entity.ClaudeUsageLog;
import com.listifyai.exception.ClaudeApiException;
import com.listifyai.repository.ClaudeUsageLogRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ClaudeResilienceTest {

    @Autowired
    private ClaudeService claudeService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private ClaudeUsageLogRepository claudeUsageLogRepository;

    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        ReflectionTestUtils.setField(claudeService, "httpClient", mockHttpClient);
        circuitBreakerRegistry.circuitBreaker("claude").transitionToClosedState();
        claudeUsageLogRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        circuitBreakerRegistry.circuitBreaker("claude").transitionToClosedState();
    }

    @Test
    void testInputTokenGuard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 41000; i++) {
            sb.append("a");
        }
        ProductInputDTO largeInput = new ProductInputDTO(sb.toString(), "wood", "large", "anyone", "any", "any", "neutral", "etsy", null);

        CompletableFuture<?> future = claudeService.generateEtsy(largeInput);
        
        ClaudeApiException ex = (ClaudeApiException) assertThrows(Exception.class, future::join).getCause();
        assertTrue(ex.getMessage().contains("too large"));

        ClaudeUsageLog log = claudeUsageLogRepository.findAll().get(0);
        assertEquals(ClaudeFailureReason.VALIDATION_FAILED, log.getFailureReason());
        verifyNoInteractions(mockHttpClient);
    }
}
