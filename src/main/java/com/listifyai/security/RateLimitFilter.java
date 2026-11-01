package com.listifyai.security;

import com.listifyai.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.time.Duration;

@Component
@SuppressWarnings({"null", "unchecked", "unused"})
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/generate",
            "/api/bulk/upload",
            "/api/analyze-etsy-listing",
            "/api/regenerate-title",
            "/api/regenerate-tags",
            "/api/regenerate-description",
            "/api/shop-health/compute",
            "/api/public/reviews"
    );

    private static final String PUBLIC_REVIEWS_PATH = "/api/public/reviews";

    private static final String TOO_MANY_REQUESTS_BODY = """
            {"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Try again in a moment."}
            """;

    private final DistributedRateLimiter limiter;
    private final TrustedProxyClientIpResolver ipResolver;
    private final int requestsPerMinute;
    private final int publicReviewRequestsPerMinute;

    public RateLimitFilter(DistributedRateLimiter limiter, TrustedProxyClientIpResolver ipResolver,
                           @Value("${rate.limit.requests.per.minute:10}") int requestsPerMinute,
                           @Value("${rate.limit.public-reviews.requests.per.minute:3}") int publicReviewRequestsPerMinute) {
        this.limiter=limiter; this.ipResolver=ipResolver;
        this.requestsPerMinute = requestsPerMinute;
        this.publicReviewRequestsPerMinute = publicReviewRequestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }

        return !isRateLimitedPath(path);
    }

    private boolean isRateLimitedPath(String path) {
        return RATE_LIMITED_PATHS.contains(path)
                || path.startsWith("/api/monitoring/recommendations/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        User user = getCurrentUserOrNull();
        if (isPublicReviewPath(request)) {
            consumeOrReject(response, "tagwing:rl:public-review:" + ipResolver.resolve(request), publicReviewRequestsPerMinute);
            if (!response.isCommitted()) filterChain.doFilter(request, response);
            return;
        }

        if (user == null || user.getId() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String category = request.getServletPath().startsWith("/api/bulk") ? "bulk" : request.getServletPath().startsWith("/api/monitoring") ? "monitoring-ai" : "ai";
        consumeOrReject(response, "tagwing:rl:user:" + user.getId() + ":" + category, requestsPerMinute);
        if (!response.isCommitted()) filterChain.doFilter(request, response);
    }

    private void consumeOrReject(HttpServletResponse response, String key, int limit) throws IOException {
        try { DistributedRateLimiter.Result result=limiter.consume(key, limit, Duration.ofMinutes(1)); if(result.allowed()) return; response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds())); writeTooManyRequests(response); }
        catch (RateLimitUnavailableException ex) { response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value()); response.setContentType(MediaType.APPLICATION_JSON_VALUE); response.getWriter().write("{\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"Request protection is temporarily unavailable. Try again shortly.\"}"); }
    }
    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(TOO_MANY_REQUESTS_BODY.strip());
    }


    private User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    private boolean isPublicReviewPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        return PUBLIC_REVIEWS_PATH.equals(path);
    }

}
