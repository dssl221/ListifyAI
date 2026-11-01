package com.listifyai.service;

import com.listifyai.entity.User;
import com.listifyai.exception.PlanLimitException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import com.listifyai.security.DistributedRateLimiter;
import com.listifyai.exception.ServiceUnavailableException;

@Component
public class PlanClaudeRateLimiter {

    private static final Map<String, Integer> HOURLY_LIMITS = Map.of(
            "free", 5,
            "starter", 20,
            "growth", 50, "pro", 200
    );

    private final DistributedRateLimiter limiter;
    public PlanClaudeRateLimiter(DistributedRateLimiter limiter) { this.limiter=limiter; }

    public void checkLimit(User user) {
        String plan = user.getPlan() == null ? "free" : user.getPlan().toLowerCase(Locale.ROOT);
        int limit = HOURLY_LIMITS.getOrDefault(plan, HOURLY_LIMITS.get("free"));
        try { var r=limiter.consume("tagwing:rl:claude:"+user.getId()+":"+plan, limit, java.time.Duration.ofHours(1)); if(!r.allowed()) throw new PlanLimitException("AI generation limit reached. Resets in " + Math.max(1,r.retryAfterSeconds()/60) + " minutes."); }
        catch(com.listifyai.security.RateLimitUnavailableException ex){throw new ServiceUnavailableException("AI request protection is temporarily unavailable. Please retry shortly.",ex);}
    }
}
