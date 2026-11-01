package com.listifyai.service;

import com.listifyai.entity.User;
import com.listifyai.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UsageService usageService;

    @Test
    void findOrCreateBySupabaseIdReusesExistingSupabaseUser() {
        User existing = user("supabase-id", "user@example.com");
        UserService userService = new UserService(userRepository, usageService);

        when(userRepository.findBySupabaseId("supabase-id")).thenReturn(Optional.of(existing));

        User result = userService.findOrCreateBySupabaseId("supabase-id", "user@example.com");

        assertSame(existing, result);
        verify(userRepository, never()).findByEmail("user@example.com");
        verify(userRepository, never()).save(existing);
    }

    @Test
    void findOrCreateBySupabaseIdReusesExistingEmailWhenSupabaseIdChanges() {
        User existing = user("old-supabase-id", "user@example.com");
        UserService userService = new UserService(userRepository, usageService);

        when(userRepository.findBySupabaseId("new-supabase-id")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = userService.findOrCreateBySupabaseId("new-supabase-id", " User@Example.com ");

        assertSame(existing, result);
        assertEquals("new-supabase-id", existing.getSupabaseId());
        assertEquals("user@example.com", existing.getEmail());
        verify(userRepository).save(existing);
    }

    @Test
    void findOrCreateBySupabaseIdMarksBrandNewUsersAsNeedingNamePrompt() {
        UserService userService = new UserService(userRepository, usageService);

        when(userRepository.findBySupabaseId("new-supabase-id")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrCreateBySupabaseId("new-supabase-id", "new@example.com");

        assertFalse(result.getNamePromptCompleted());
        assertEquals("new@example.com", result.getEmail());
    }

    @Test
    void updateDisplayNameSavesNameAndCompletesPrompt() {
        User user = user("supabase-id", "user@example.com");
        user.setNamePromptCompleted(false);
        UserService userService = new UserService(userRepository, usageService);

        when(userRepository.save(user)).thenReturn(user);
        when(usageService.getMonthlyLimit(user.getPlan())).thenReturn(5);
        when(usageService.getRemainingCredits(user)).thenReturn("5");

        var profile = userService.updateDisplayName(user, "  Debojit   Roy  ");

        assertEquals("Debojit Roy", user.getDisplayName());
        assertTrue(user.getNamePromptCompleted());
        assertEquals("Debojit Roy", profile.displayName());
        assertTrue(profile.namePromptCompleted());
    }

    private User user(String supabaseId, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setSupabaseId(supabaseId);
        user.setEmail(email);
        user.setNamePromptCompleted(true);
        user.setPlan("free");
        user.setBillingCycleStart(LocalDate.now());
        return user;
    }
}
