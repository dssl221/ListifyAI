package com.listifyai.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {
    private RateLimitFilter filter(int limit) {
        DistributedRateLimiter limiter = mock(DistributedRateLimiter.class);
        when(limiter.consume(anyString(), anyInt(), any())).thenReturn(new DistributedRateLimiter.Result(true, 60));
        return new RateLimitFilter(limiter, new TrustedProxyClientIpResolver("10.0.0.1"), limit, limit);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void limitsPublicReviewPostsByClientIp() throws Exception {
        RateLimitFilter filter = filter(1);

        MockHttpServletResponse firstResponse = doPublicReviewPost(filter, "203.0.113.10", null);
        MockHttpServletResponse secondResponse = doPublicReviewPost(filter, "203.0.113.10", null);

        assertEquals(200, firstResponse.getStatus());
        assertEquals(200, secondResponse.getStatus());
    }

    @Test
    void usesFirstForwardedIpForPublicReviewLimit() throws Exception {
        RateLimitFilter filter = filter(1);

        MockHttpServletResponse firstResponse = doPublicReviewPost(filter, "10.0.0.1", "198.51.100.20, 10.0.0.1");
        MockHttpServletResponse secondResponse = doPublicReviewPost(filter, "10.0.0.2", "198.51.100.20, 10.0.0.2");

        assertEquals(200, firstResponse.getStatus());
        assertEquals(200, secondResponse.getStatus());
    }

    @Test
    void doesNotLimitPublicReviewGets() throws Exception {
        RateLimitFilter filter = filter(1);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/reviews");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void returns429AndRetryAfterWhenRedisLimitIsExceeded() throws Exception {
        DistributedRateLimiter limiter = mock(DistributedRateLimiter.class);
        when(limiter.consume(anyString(), anyInt(), any())).thenReturn(new DistributedRateLimiter.Result(false, 42));
        RateLimitFilter filter = new RateLimitFilter(limiter, new TrustedProxyClientIpResolver(""), 10, 1);
        MockHttpServletResponse response = doPublicReviewPost(filter, "203.0.113.10", null);
        assertEquals(429, response.getStatus());
        assertEquals("42", response.getHeader("Retry-After"));
    }

    @Test
    void ignoresSpoofedForwardedHeaderUnlessRemoteAddressIsTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest(); request.setRemoteAddr("198.51.100.10"); request.addHeader("X-Forwarded-For", "1.2.3.4");
        assertEquals("198.51.100.10", new TrustedProxyClientIpResolver("10.0.0.1").resolve(request));
        request.setRemoteAddr("10.0.0.1");
        assertEquals("1.2.3.4", new TrustedProxyClientIpResolver("10.0.0.1").resolve(request));
    }

    private MockHttpServletResponse doPublicReviewPost(RateLimitFilter filter, String remoteAddr, String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/public/reviews");
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
