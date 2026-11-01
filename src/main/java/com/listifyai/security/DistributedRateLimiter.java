package com.listifyai.security;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.time.Duration; import java.util.List;

/** Redis INCR+PEXPIRE in one script: no JVM-local identity maps and every key expires. */
@Component
public class DistributedRateLimiter {
    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>("""
        local current = redis.call('INCR', KEYS[1])
        if current == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
        local ttl = redis.call('PTTL', KEYS[1])
        return {current, ttl}
        """, List.class);
    private final StringRedisTemplate redis;
    public DistributedRateLimiter(StringRedisTemplate redis) { this.redis = redis; }
    public Result consume(String key, int limit, Duration window) {
        try {
            List<?> result = redis.execute(SCRIPT, List.of(key), String.valueOf(window.toMillis()));
            long used=((Number)result.get(0)).longValue(), ttl=((Number)result.get(1)).longValue();
            return new Result(used <= limit, Math.max(1, (ttl + 999) / 1000));
        } catch (RedisConnectionFailureException ex) { throw new RateLimitUnavailableException(ex); }
    }
    public record Result(boolean allowed, long retryAfterSeconds) {}
}
