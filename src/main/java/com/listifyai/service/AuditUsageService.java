package com.listifyai.service;

import com.listifyai.dto.EtsyAuditDtos.AuditUsageDTO;
import com.listifyai.entity.AuditUsage;
import com.listifyai.entity.User;
import com.listifyai.exception.ListingLimitException;
import com.listifyai.repository.AuditUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@Service
public class AuditUsageService {

    private static final Map<String, Integer> PLAN_LIMITS = Map.of(
            "free", 2,
            "starter", 10,
            "growth", 100,
            "pro", 500
    );

    private final AuditUsageRepository auditUsageRepository;

    public AuditUsageService(AuditUsageRepository auditUsageRepository) {
        this.auditUsageRepository = auditUsageRepository;
    }

    @Transactional
    public AuditUsageDTO checkAndIncrement(User user) {
        AuditUsage usage = getOrCreateCurrentUsage(user);
        int limit = limitFor(user.getPlan());
        if (usage.getAuditsUsed() >= limit) {
            throw new ListingLimitException("You have used your monthly audit credits. Upgrade to audit more listings.");
        }
        usage.setAuditsUsed(usage.getAuditsUsed() + 1);
        auditUsageRepository.save(usage);
        return toDto(user, usage);
    }

    @Transactional
    public AuditUsageDTO getUsage(User user) {
        return toDto(user, getOrCreateCurrentUsage(user));
    }

    private AuditUsage getOrCreateCurrentUsage(User user) {
        LocalDate periodStart = YearMonth.now().atDay(1);
        return auditUsageRepository.findByUserIdAndPeriodStart(user.getId(), periodStart)
                .orElseGet(() -> {
                    AuditUsage usage = new AuditUsage();
                    usage.setUser(user);
                    usage.setPeriodStart(periodStart);
                    usage.setResetDate(periodStart.plusMonths(1));
                    usage.setAuditsUsed(0);
                    return auditUsageRepository.save(usage);
                });
    }

    private AuditUsageDTO toDto(User user, AuditUsage usage) {
        int limit = limitFor(user.getPlan());
        int used = usage.getAuditsUsed();
        return new AuditUsageDTO(
                plan(user.getPlan()),
                used,
                limit,
                Math.max(0, limit - used),
                usage.getResetDate()
        );
    }

    private int limitFor(String plan) {
        return PLAN_LIMITS.getOrDefault(plan(plan), 2);
    }

    private String plan(String plan) {
        return plan == null || plan.isBlank() ? "free" : plan.toLowerCase();
    }
}
