package com.listifyai.security;

import com.listifyai.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SupabaseJwtFilterTest {

    private static final String RAW_SECRET = "raw-secret-that-is-at-least-32-bytes!!";
    private static final String BASE64_LOOKING_SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    @Mock
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void buildSigningKeyUsesSupabaseSecretAsRawUtf8Bytes() {
        SecretKey key = SupabaseJwtFilter.buildSigningKey(BASE64_LOOKING_SECRET);

        assertArrayEquals(
                BASE64_LOOKING_SECRET.getBytes(StandardCharsets.UTF_8),
                key.getEncoded()
        );
    }

    @Test
    void buildSigningKeyFallsBackToRawBytesForNonBase64Secret() {
        SecretKey key = SupabaseJwtFilter.buildSigningKey(RAW_SECRET);

        assertArrayEquals(RAW_SECRET.getBytes(StandardCharsets.UTF_8), key.getEncoded());
    }

    @Test
    void filterReturns401WhenAuthorizationHeaderIsMissing() throws Exception {
        SupabaseJwtFilter filter = new SupabaseJwtFilter(userService, BASE64_LOOKING_SECRET, "");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/listings");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void filterReturns401WhenTokenIsExpired() throws Exception {
        SupabaseJwtFilter filter = new SupabaseJwtFilter(userService, BASE64_LOOKING_SECRET, "");
        String expiredToken = Jwts.builder()
                .setSubject("supabase-id")
                .claim("email", "user@example.com")
                .setExpiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(SupabaseJwtFilter.buildSigningKey(BASE64_LOOKING_SECRET), SignatureAlgorithm.HS256)
                .compact();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/listings");
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void filterReturns401WhenTokenSubjectIsMissing() throws Exception {
        SupabaseJwtFilter filter = new SupabaseJwtFilter(userService, BASE64_LOOKING_SECRET, "");
        String tokenWithoutSubject = Jwts.builder()
                .claim("email", "user@example.com")
                .setExpiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(SupabaseJwtFilter.buildSigningKey(BASE64_LOOKING_SECRET), SignatureAlgorithm.HS256)
                .compact();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/listings");
        request.addHeader("Authorization", "Bearer " + tokenWithoutSubject);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(userService);
    }
}
