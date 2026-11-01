package com.listifyai.service;

import com.listifyai.entity.User;
import com.listifyai.entity.UserUsage;
import com.listifyai.exception.ListingLimitException;
import com.listifyai.repository.UserRepository;
import com.listifyai.repository.UserUsageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Spy;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserUsageRepository userUsageRepository;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private UsageService usageService;

    @Test
    void checkAndIncrementThrowsListingLimitExceptionWhenIncrementUsageAtomicallyReturnsZero() {
        User user = user("free");

        when(userUsageRepository.incrementUsageAtomically(eq(user.getId()), any(LocalDate.class), any(LocalDate.class), eq(5))).thenReturn(0);

        assertThrows(ListingLimitException.class, () -> usageService.checkAndIncrement(user));

        verify(userUsageRepository).ensureUsageRecord(eq(user.getId()), any(LocalDate.class), any(LocalDate.class), eq(5));
        verify(userUsageRepository).incrementUsageAtomically(eq(user.getId()), any(LocalDate.class), any(LocalDate.class), eq(5));
    }

    @Test
    void checkAndIncrementSucceedsWhenIncrementReturnsOne() {
        User user = user("free");

        when(userUsageRepository.incrementUsageAtomically(eq(user.getId()), any(LocalDate.class), any(LocalDate.class), eq(5))).thenReturn(1);

        assertDoesNotThrow(() -> usageService.checkAndIncrement(user));

        verify(userUsageRepository).ensureUsageRecord(eq(user.getId()), any(LocalDate.class), any(LocalDate.class), eq(5));
        verify(userUsageRepository).incrementUsageAtomically(eq(user.getId()), any(LocalDate.class), any(LocalDate.class), eq(5));
    }

    @Test
    void checkAndIncrementSkipsDbCallEntirelyForUnlimitedPlans() {
        usageService.checkAndIncrement(user("pro"));

        verifyNoInteractions(userUsageRepository);
    }

    @Test
    void isUnlimitedPlanReturnsTrueOnlyForPro() {
        assertFalse(usageService.isUnlimitedPlan("growth"));
        assertTrue(usageService.isUnlimitedPlan("pro"));
    }

    @Test
    void getRemainingCreditsReturnsUnlimitedForProPlan() {
        assertEquals("Unlimited", usageService.getRemainingCredits(user("pro")));
    }

    @Test
    void getRemainingCreditsReturnsCorrectNumberForFreePlan() {
        User user = user("free");
        UserUsage mockUsage = new UserUsage();
        mockUsage.setUsedCount(2);
        
        when(userUsageRepository.findByUserIdAndPeriodStartAndPeriodEnd(eq(user.getId()), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Optional.of(mockUsage));

        assertEquals("3", usageService.getRemainingCredits(user));
    }

    @Test
    void resetMonthlyUsagePreCreatesRecords() {
        User user1 = user("free");
        User user2 = user("pro");
        
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        usageService.resetMonthlyUsage();

        verify(userUsageRepository, times(1)).ensureUsageRecord(eq(user1.getId()), any(LocalDate.class), any(LocalDate.class), eq(5));
        verify(userUsageRepository, never()).ensureUsageRecord(eq(user2.getId()), any(LocalDate.class), any(LocalDate.class), anyInt());
    }

    private User user(String plan) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setSupabaseId("supabase-id");
        user.setPlan(plan);
        return user;
    }
}
