package com.listifyai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.dto.EtsyAuditDtos.CompactEtsyListing;
import com.listifyai.exception.EtsyTransientException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class EtsyApiClientResilienceTest {

    @Autowired
    private EtsyApiClient etsyApiClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        ReflectionTestUtils.setField(etsyApiClient, "httpClient", mockHttpClient);
        ReflectionTestUtils.setField(etsyApiClient, "apiKey", "valid_test_key");
        circuitBreakerRegistry.circuitBreaker("etsyApi").transitionToClosedState();
    }

    @AfterEach
    void tearDown() {
        circuitBreakerRegistry.circuitBreaker("etsyApi").transitionToClosedState();
    }

    @Test
    void testValidListing() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("""
            {
              "title": "Wooden Table",
              "description": "A nice table",
              "tags": ["wood", "table"],
              "state": "active",
              "price": {"amount": "100", "currency_code": "USD"}
            }
            """);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        CompactEtsyListing result = etsyApiClient.fetchListing("123", "https://etsy.com/listing/123");
        assertNotNull(result);
        assertEquals("Wooden Table", result.title());

        verify(mockHttpClient, times(1)).send(any(), any());
    }

    @Test
    void test404NotFoundDoesNotRetry() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                etsyApiClient.fetchListing("123", "https://etsy.com/listing/123"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Etsy listing not found.", ex.getReason());

        // Should not retry on 404
        verify(mockHttpClient, times(1)).send(any(), any());
    }

    @Test
    void test429RateLimitRetriesAndThrowsTransient() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(429);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        EtsyTransientException ex = assertThrows(EtsyTransientException.class, () ->
                etsyApiClient.fetchListing("123", "https://etsy.com/listing/123"));

        assertEquals("Etsy is temporarily rate-limiting requests. Please try again later.", ex.getMessage());

        // Should retry up to maxAttempts (3 times)
        verify(mockHttpClient, times(3)).send(any(), any());
    }

    @Test
    void test500UpstreamErrorRetriesAndThrowsTransient() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(502);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        EtsyTransientException ex = assertThrows(EtsyTransientException.class, () ->
                etsyApiClient.fetchListing("123", "https://etsy.com/listing/123"));

        assertEquals("Could not fetch Etsy listing data. Please try again.", ex.getMessage());

        // Should retry up to maxAttempts (3 times)
        verify(mockHttpClient, times(3)).send(any(), any());
    }

    @Test
    void testTimeoutRetriesAndThrowsTransient() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("Timeout"));

        EtsyTransientException ex = assertThrows(EtsyTransientException.class, () ->
                etsyApiClient.fetchListing("123", "https://etsy.com/listing/123"));

        assertEquals("Could not fetch Etsy listing data. Please try again.", ex.getMessage());

        // Should retry up to maxAttempts (3 times)
        verify(mockHttpClient, times(3)).send(any(), any());
    }
}
