package com.listifyai.service;

import com.listifyai.entity.User;
import com.listifyai.entity.UserUsage;
import com.listifyai.exception.ListingLimitException;
import com.listifyai.repository.UserRepository;
import com.listifyai.repository.UserUsageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class UsageIntegrationTest {

    @Autowired
    private UsageService usageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserUsageRepository userUsageRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("usage-integration-" + UUID.randomUUID() + "@example.com");
        testUser.setPlan("free");
        testUser.setSupabaseId(UUID.randomUUID().toString());
        testUser = userRepository.save(testUser);

        LocalDate today = LocalDate.now();
        LocalDate periodStart = LocalDate.of(today.getYear(), today.getMonth(), 1);
        LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
        UserUsage usage = new UserUsage();
        usage.setUser(testUser);
        usage.setPeriodStart(periodStart);
        usage.setPeriodEnd(periodEnd);
        usage.setUsedCount(0);
        usage.setPlanLimit(5);
        userUsageRepository.save(usage);
    }

    @AfterEach
    void tearDown() {
        userUsageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testConcurrentUsageEnforcement_FreePlan() throws InterruptedException {
        // Free plan has a limit of 5. We will make 10 concurrent requests.
        int numThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                try {
                    usageService.checkAndIncrement(testUser);
                    successCount.incrementAndGet();
                } catch (ListingLimitException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Ensure exactly 5 succeeded and 5 failed
        assertEquals(5, successCount.get(), "Exactly 5 requests should succeed");
        assertEquals(5, failureCount.get(), "Exactly 5 requests should fail due to rate limit");

        LocalDate today = LocalDate.now();
        LocalDate periodStart = LocalDate.of(today.getYear(), today.getMonth(), 1);
        LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
        UserUsage usage = userUsageRepository.findByUserIdAndPeriodStartAndPeriodEnd(testUser.getId(), periodStart, periodEnd).orElseThrow();
        assertEquals(5, usage.getUsedCount());
    }

    @Test
    void testUsageEnforcement_StarterPlan() {
        testUser.setPlan("starter");
        userRepository.save(testUser);

        // Limit is 30. Should be able to make 30 requests successfully.
        for (int i = 0; i < 30; i++) {
            usageService.checkAndIncrement(testUser);
        }

        // 31st request should fail
        Exception e = null;
        try {
            usageService.checkAndIncrement(testUser);
        } catch (ListingLimitException ex) {
            e = ex;
        }
        assertEquals(ListingLimitException.class, e.getClass());
    }

    @Test
    void testUsageEnforcement_GrowthPlan() {
        testUser.setPlan("growth");
        userRepository.save(testUser);

        // Limit is 100. Should be able to make 100 requests successfully.
        for (int i = 0; i < 100; i++) {
            usageService.checkAndIncrement(testUser);
        }

        // 101st request should fail
        Exception e = null;
        try {
            usageService.checkAndIncrement(testUser);
        } catch (ListingLimitException ex) {
            e = ex;
        }
        assertEquals(ListingLimitException.class, e.getClass());
    }

    @Test
    void testUsageEnforcement_ProPlan() {
        testUser.setPlan("pro");
        userRepository.save(testUser);
        
        userUsageRepository.deleteAll();

        // Pro is unlimited. We will make 1000 requests as a test.
        for (int i = 0; i < 1000; i++) {
            usageService.checkAndIncrement(testUser);
        }
        
        // No exception should be thrown
        // No usage record is even created for pro
        LocalDate today = LocalDate.now();
        LocalDate periodStart = LocalDate.of(today.getYear(), today.getMonth(), 1);
        LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
        
        boolean hasUsage = userUsageRepository.findByUserIdAndPeriodStartAndPeriodEnd(testUser.getId(), periodStart, periodEnd).isPresent();
        assertEquals(false, hasUsage);
    }
}
