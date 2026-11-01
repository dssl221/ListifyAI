package com.listifyai.service;

import com.listifyai.entity.BulkJob;
import com.listifyai.entity.BulkJobItem;
import com.listifyai.entity.User;
import com.listifyai.repository.BulkJobItemRepository;
import com.listifyai.repository.BulkJobRepository;
import com.listifyai.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class BulkJobCompletionServiceTest {

    @Autowired
    private BulkJobCompletionService completionService;

    @Autowired
    private BulkJobRepository bulkJobRepository;

    @Autowired
    private BulkJobItemRepository bulkJobItemRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private BulkJob testJob;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("completion-" + UUID.randomUUID() + "@example.com");
        testUser.setPlan("pro");
        testUser.setSupabaseId(UUID.randomUUID().toString());
        userRepository.save(testUser);

        testJob = new BulkJob();
        testJob.setUser(testUser);
        testJob.setStatus("PENDING");
        testJob.setTotalItems(2);
        bulkJobRepository.save(testJob);
    }

    @AfterEach
    void tearDown() {
        bulkJobItemRepository.deleteAll();
        bulkJobRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCountsRecomputedCorrectly() {
        BulkJobItem item1 = new BulkJobItem();
        item1.setJob(testJob);
        item1.setStatus("COMPLETED");
        item1.setInputPayload("{}");
        bulkJobItemRepository.save(item1);

        BulkJobItem item2 = new BulkJobItem();
        item2.setJob(testJob);
        item2.setStatus("FAILED");
        item2.setInputPayload("{}");
        bulkJobItemRepository.save(item2);

        completionService.checkAndCompleteJob(testJob.getId());

        BulkJob updated = bulkJobRepository.findById(testJob.getId()).orElseThrow();
        assertEquals(1, updated.getCompletedItems());
        assertEquals(1, updated.getFailedItems());
        assertEquals("COMPLETED", updated.getStatus());
        assertNotNull(updated.getCompletedAt());
    }

    @Test
    void testIdempotentCompletion() {
        BulkJobItem item1 = new BulkJobItem();
        item1.setJob(testJob);
        item1.setStatus("COMPLETED");
        item1.setInputPayload("{}");
        bulkJobItemRepository.save(item1);

        completionService.checkAndCompleteJob(testJob.getId());
        BulkJob updated = bulkJobRepository.findById(testJob.getId()).orElseThrow();
        assertEquals("COMPLETED", updated.getStatus());

        // Call again
        completionService.checkAndCompleteJob(testJob.getId());
        BulkJob updatedAgain = bulkJobRepository.findById(testJob.getId()).orElseThrow();
        assertEquals("COMPLETED", updatedAgain.getStatus());
    }

    @Test
    void testNotUpdatedIfAlreadyTerminal() {
        testJob.setStatus("COMPLETED");
        testJob.setCompletedItems(5);
        bulkJobRepository.save(testJob);

        // No items are saved, so if recomputed it would be 0
        completionService.checkAndCompleteJob(testJob.getId());

        BulkJob updated = bulkJobRepository.findById(testJob.getId()).orElseThrow();
        // Since it returned early, completed items should still be 5
        assertEquals(5, updated.getCompletedItems());
    }
}
