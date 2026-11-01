package com.listifyai.service;

import com.listifyai.dto.UserProfileDTO;
import com.listifyai.entity.User;
import com.listifyai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UsageService usageService;

    public UserService(UserRepository userRepository, UsageService usageService) {
        this.userRepository = userRepository;
        this.usageService = usageService;
    }

    public UserProfileDTO getProfile(User user) {
        boolean unlimited = usageService.isUnlimitedPlan(user.getPlan());
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                Boolean.TRUE.equals(user.getNamePromptCompleted()),
                user.getPlan(),
                user.getRole(),
                usageService.getUsedCount(user),
                usageService.getMonthlyLimit(user.getPlan()),
                unlimited,
                usageService.getRemainingCredits(user)
        );
    }

    @Transactional
    public User findOrCreateBySupabaseId(String supabaseId, String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findBySupabaseId(supabaseId)
                .map(user -> updateIdentityIfNeeded(user, supabaseId, normalizedEmail))
                .or(() -> normalizedEmail == null ? Optional.empty() : userRepository.findByEmail(normalizedEmail)
                        .map(user -> updateIdentityIfNeeded(user, supabaseId, normalizedEmail)))
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setSupabaseId(supabaseId);
                    newUser.setEmail(normalizedEmail != null ? normalizedEmail : "");
                    newUser.setNamePromptCompleted(false);
                    newUser.setPlan("free");
                    newUser.setBillingCycleStart(LocalDate.now());
                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public User findOrCreateDeveloperUser(String supabaseId, String email) {
        return userRepository.findBySupabaseId(supabaseId)
                .or(() -> userRepository.findByEmail(email))
                .map(user -> {
                    user.setSupabaseId(supabaseId);
                    user.setEmail(email);
                    user.setPlan("pro");
                    if (user.getBillingCycleStart() == null) {
                        user.setBillingCycleStart(LocalDate.now());
                    }
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setSupabaseId(supabaseId);
                    newUser.setEmail(email);
                    newUser.setNamePromptCompleted(false);
                    newUser.setPlan("pro");
                    newUser.setBillingCycleStart(LocalDate.now());
                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public User findOrCreateDeveloperAdminUser(String supabaseId, String email) {
        User user = findOrCreateDeveloperUser(supabaseId, email);
        user.setRole("admin");
        return userRepository.save(user);
    }

    @Transactional
    public User updatePlan(String stripeCustomerId, String newPlan) {
        User user = userRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseThrow(() -> new RuntimeException(
                        "No user found for Stripe customer: " + stripeCustomerId));
        user.setPlan(newPlan);
        return userRepository.save(user);
    }

    @Transactional
    public void setStripeCustomerId(User user, String stripeCustomerId) {
        user.setStripeCustomerId(stripeCustomerId);
        userRepository.save(user);
    }

    @Transactional
    public UserProfileDTO updateDisplayName(User user, String displayName) {
        String normalizedName = normalizeDisplayName(displayName);
        user.setDisplayName(normalizedName);
        user.setNamePromptCompleted(true);
        User saved = userRepository.save(user);
        return getProfile(saved);
    }

    private User updateIdentityIfNeeded(User user, String supabaseId, String email) {
        boolean changed = false;
        if (supabaseId != null && !supabaseId.equals(user.getSupabaseId())) {
            user.setSupabaseId(supabaseId);
            changed = true;
        }
        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            changed = true;
        }
        if (user.getBillingCycleStart() == null) {
            user.setBillingCycleStart(LocalDate.now());
            changed = true;
        }
        return changed ? userRepository.save(user) : user;
    }

    private String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name is required.");
        }

        String normalized = displayName.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 50) {
            throw new IllegalArgumentException("Display name must be 50 characters or fewer.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
