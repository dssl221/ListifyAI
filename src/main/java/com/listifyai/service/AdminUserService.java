package com.listifyai.service;

import com.listifyai.dto.AdminDtos.*;
import com.listifyai.entity.User;
import com.listifyai.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@SuppressWarnings({"null", "unchecked", "unused"})
public class AdminUserService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final BulkJobRepository bulkJobRepository;
    private final MonitoredListingRepository monitoredListingRepository;
    private final AnalyzedListingRepository analyzedListingRepository;
    private final ClaudeUsageLogRepository claudeUsageLogRepository;
    private final UsageService usageService;
    private final AdminAuditService adminAuditService;

    public AdminUserService(UserRepository userRepository,
                            ListingRepository listingRepository,
                            BulkJobRepository bulkJobRepository,
                            MonitoredListingRepository monitoredListingRepository,
                            AnalyzedListingRepository analyzedListingRepository,
                            ClaudeUsageLogRepository claudeUsageLogRepository,
                            UsageService usageService,
                            AdminAuditService adminAuditService) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.bulkJobRepository = bulkJobRepository;
        this.monitoredListingRepository = monitoredListingRepository;
        this.analyzedListingRepository = analyzedListingRepository;
        this.claudeUsageLogRepository = claudeUsageLogRepository;
        this.usageService = usageService;
        this.adminAuditService = adminAuditService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserListItemDTO> getAllUsers(String search, String plan, int page, int size) {
        Specification<User> spec = (root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String term = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), term),
                        cb.like(cb.lower(root.get("displayName")), term)
                ));
            }
            if (plan != null && !plan.isBlank()) {
                predicates.add(cb.equal(root.get("plan"), plan));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return userRepository.findAll(spec, PageRequest.of(page, size)).map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDTO getUserDetail(UUID userId) {
        User user = load(userId);
        return new AdminUserDetailDTO(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getPlan(), user.getRole(),
                safeInt(usageService.getUsedCount(user)), usageService.getMonthlyLimit(user.getPlan()),
                user.getCreatedAt(), user.getUpdatedAt(), user.getStripeCustomerId(), user.getStripeSubscriptionId(),
                listingRepository.countByUserId(userId),
                bulkJobRepository.countByUserId(userId),
                monitoredListingRepository.countByUserId(userId),
                analyzedListingRepository.countByUserId(userId),
                nvl(claudeUsageLogRepository.sumTokensByUserId(userId))
        );
    }

    public void suspendUser(UUID targetId, User admin, HttpServletRequest request) {
        User target = load(targetId);
        if ("admin".equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin users cannot be suspended");
        }
        target.setRole("suspended");
        userRepository.save(target);
        adminAuditService.log(admin, "USER_SUSPENDED", "user", targetId, Map.of("email", target.getEmail()), request);
    }

    public void reactivateUser(UUID targetId, User admin, HttpServletRequest request) {
        User target = load(targetId);
        target.setRole("user");
        userRepository.save(target);
        adminAuditService.log(admin, "USER_REACTIVATED", "user", targetId, Map.of("email", target.getEmail()), request);
    }

    public void changePlan(UUID targetId, String newPlan, User admin, HttpServletRequest request) {
        if (!Set.of("free", "starter", "growth", "pro").contains(newPlan)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid plan");
        }
        User target = load(targetId);
        String oldPlan = target.getPlan();
        target.setPlan(newPlan);
        userRepository.save(target);
        adminAuditService.log(admin, "PLAN_CHANGED", "user", targetId, Map.of("from", oldPlan, "to", newPlan), request);
    }

    public void makeAdmin(UUID targetId, User admin, HttpServletRequest request) {
        User target = load(targetId);
        target.setRole("admin");
        userRepository.save(target);
        adminAuditService.log(admin, "ROLE_PROMOTED_TO_ADMIN", "user", targetId, Map.of("email", target.getEmail()), request);
    }

    private User load(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AdminUserListItemDTO toListItem(User user) {
        return new AdminUserListItemDTO(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getPlan(), user.getRole(),
                safeInt(usageService.getUsedCount(user)), user.getCreatedAt(), user.getUpdatedAt(),
                user.getStripeCustomerId(), user.getStripeSubscriptionId() != null && !user.getStripeSubscriptionId().isBlank()
        );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long nvl(Long value) {
        return value == null ? 0 : value;
    }
}
