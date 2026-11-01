package com.listifyai.security;

import com.listifyai.entity.User;
import com.listifyai.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.sentry.Sentry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;

@Component
@SuppressWarnings({"null", "unchecked", "unused"})
public class SupabaseJwtFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final SecretKey signingKey;
    private final Set<String> adminEmails;

    public SupabaseJwtFilter(
            UserService userService,
            @Value("${supabase.jwt.secret}") String jwtSecret,
            @Value("${admin.emails:}") String adminEmails) {
        this.userService = userService;
        this.signingKey = buildSigningKey(jwtSecret);
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (isProtectedApiRequest(request)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing bearer token.");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String supabaseId = claims.getSubject();
            String email = claims.get("email", String.class);
            if (supabaseId == null || supabaseId.isBlank()) {
                throw new JwtException("JWT subject is required.");
            }

            User user = userService.findOrCreateBySupabaseId(supabaseId, email);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            if ("admin".equalsIgnoreCase(user.getRole()) || (email != null && adminEmails.contains(email.toLowerCase()))) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Set MDC and Sentry User Context
            MDC.put("userId", user.getId().toString());
            io.sentry.protocol.User sentryUser = new io.sentry.protocol.User();
            sentryUser.setId(user.getId().toString());
            sentryUser.setEmail(user.getEmail());
            Sentry.setUser(sentryUser);

        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired bearer token.");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
        }
    }

    static SecretKey buildSigningKey(String jwtSecret) {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isProtectedApiRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }

        if (path == null || !path.startsWith("/api/")) {
            return false;
        }

        return !(path.startsWith("/api/public/")
                || ("POST".equalsIgnoreCase(method) && path.startsWith("/api/auth/"))
                || ("POST".equalsIgnoreCase(method) && "/api/billing/webhook".equals(path)));
    }
}
