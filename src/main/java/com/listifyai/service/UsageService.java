package com.listifyai.service;

import com.listifyai.entity.User;
import com.listifyai.entity.UserUsage;
import com.listifyai.exception.ListingLimitException;
import com.listifyai.repository.UserRepository;
import com.listifyai.repository.UserUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.List;
import io.micrometer.core.instrument.MeterRegistry;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class UsageService {

    private static final Logger log = LoggerFactory.getLogger(UsageService.class);

    private static final Map<String, Integer> PLAN_LIMITS = Map.of(
            "free", 5,
            "starter", 30,
            "growth", 100,
            "pro", Integer.MAX_VALUE
    );

    public static final String UNLIMITED_CREDITS = "Unlimited";

    private final UserRepository userRepository;
    private final UserUsageRepository userUsageRepository;
    private final MeterRegistry meterRegistry;

    public UsageService(UserRepository userRepository, UserUsageRepository userUsageRepository, MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.userUsageRepository = userUsageRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public void checkAndIncrement(User user) {
        int limit = getPlanLimit(user.getPlan());

        if (limit == Integer.MAX_VALUE) {
            return;
        }

        LocalDate start = YearMonth.now().atDay(1);
        LocalDate end = YearMonth.now().atEndOfMonth();

        userUsageRepository.ensureUsageRecord(user.getId(), start, end, limit);
        int updatedRows = userUsageRepository.incrementUsageAtomically(user.getId(), start, end, limit);
        if (updatedRows == 0) {
            meterRegistry.counter("listifyai.usage.blocked", "type", "claude").increment();
            throw new ListingLimitException("Monthly limit reached. Upgrade your plan.");
        }
    }

    public String getRemainingCredits(User user) {
        int limit = getPlanLimit(user.getPlan());
        if (limit == Integer.MAX_VALUE) {
            return UNLIMITED_CREDITS;
        }
        
        LocalDate start = YearMonth.now().atDay(1);
        LocalDate end = YearMonth.now().atEndOfMonth();
        
        return userUsageRepository.findByUserIdAndPeriodStartAndPeriodEnd(user.getId(), start, end)
                .map(usage -> String.valueOf(Math.max(0, limit - usage.getUsedCount())))
                .orElse(String.valueOf(limit));
    }

    public int getUsedCount(User user) {
        LocalDate start = YearMonth.now().atDay(1);
        LocalDate end = YearMonth.now().atEndOfMonth();
        return userUsageRepository.findByUserIdAndPeriodStartAndPeriodEnd(user.getId(), start, end)
                .map(UserUsage::getUsedCount)
                .orElse(0);
    }

    public int getMonthlyLimit(String plan) {
        int limit = getPlanLimit(plan);
        return limit == Integer.MAX_VALUE ? 0 : limit;
    }

    public boolean isUnlimitedPlan(String plan) {
        return "pro".equalsIgnoreCase(plan);
    }

    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void resetMonthlyUsage() {
        log.info("Pre-creating usage records for the new month...");
        LocalDate start = YearMonth.now().atDay(1);
        LocalDate end = YearMonth.now().atEndOfMonth();
        
        List<User> users = userRepository.findAll();
        for (User user : users) {
            int limit = getPlanLimit(user.getPlan());
            if (limit != Integer.MAX_VALUE) {
                userUsageRepository.ensureUsageRecord(user.getId(), start, end, limit);
            }
        }
        log.info("Monthly usage pre-creation completed for {} users", users.size());
    }

    private int getPlanLimit(String plan) {
        return PLAN_LIMITS.getOrDefault(plan != null ? plan.toLowerCase() : "free", 5);
    }
}
