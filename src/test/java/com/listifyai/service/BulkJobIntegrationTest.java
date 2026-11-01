package com.listifyai.service;

import com.listifyai.dto.BulkJobStatusDTO;
import com.listifyai.dto.EtsyOutputDTO;
import com.listifyai.entity.BulkJob;
import com.listifyai.entity.BulkJobItem;
import com.listifyai.entity.User;
import com.listifyai.repository.BulkJobItemRepository;
import com.listifyai.repository.BulkJobRepository;
import com.listifyai.repository.ListingRepository;
import com.listifyai.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
class BulkJobIntegrationTest {

    @Autowired
    private BulkJobService bulkJobService;

    @Autowired
    private BulkJobProcessor bulkJobProcessor;

    @Autowired
    private BulkJobRepository bulkJobRepository;

    @Autowired
    private BulkJobItemRepository bulkJobItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @MockBean
    private ClaudeService claudeService;

    // Redis is an external dependency; these tests exercise job persistence and processing.
    @MockBean
    private PlanClaudeRateLimiter planClaudeRateLimiter;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("bulk-jobs-" + UUID.randomUUID() + "@example.com");
        testUser.setPlan("pro");
        testUser.setSupabaseId(UUID.randomUUID().toString());
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        bulkJobItemRepository.deleteAll();
        bulkJobRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    private MockMultipartFile createCsvFile(String content) {
        return new MockMultipartFile("file", "test.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testJobCreationWorks() throws IOException {
        String csv = "productName,material,size,targetBuyer,useCase,occasion,platform,tone\n" +
                "Mug,Ceramic,11oz,Everyone,Coffee,Any,etsy,casual\n" +
                "Shirt,Cotton,L,Men,Wear,Summer,etsy,fun\n";

        BulkJobStatusDTO jobDto = bulkJobService.uploadAndQueue(createCsvFile(csv), testUser);
        assertNotNull(jobDto);
        assertEquals(2, jobDto.totalRows());
        assertEquals("PENDING", jobDto.status());
    }

    @Test
    void testJobProgressUpdates() throws IOException {
        String csv = "productName,material,size,targetBuyer,useCase,occasion,platform,tone\n" +
                "Mug,Ceramic,11oz,Everyone,Coffee,Any,etsy,casual\n" +
                "Shirt,Cotton,L,Men,Wear,Summer,etsy,fun\n";

        BulkJobStatusDTO jobDto = bulkJobService.uploadAndQueue(createCsvFile(csv), testUser);
        BulkJob job = bulkJobRepository.findById(jobDto.jobId()).orElseThrow();

        List<BulkJobItem> items = bulkJobItemRepository.findByJobId(job.getId());
        assertEquals(2, items.size());

        // Mock ClaudeService
        EtsyOutputDTO mockEtsy = new EtsyOutputDTO(List.of("Title"), "Desc", List.of(), 100, List.of("tip"));
        when(claudeService.generateEtsy(any())).thenReturn(CompletableFuture.completedFuture(mockEtsy));

        // Process first item
        bulkJobProcessor.processSingleItem(items.get(0));

        // Update counts in job (in reality, a scheduled task handles this update, but we can verify item status)
        BulkJobItem updatedItem1 = bulkJobItemRepository.findById(items.get(0).getId()).orElseThrow();
        assertEquals("COMPLETED", updatedItem1.getStatus());
        assertNotNull(updatedItem1.getResultReference());
    }

    @Test
    void testPartialFailureHandled() throws IOException {
        String csv = "productName,material,size,targetBuyer,useCase,occasion,platform,tone\n" +
                "Mug,Ceramic,11oz,Everyone,Coffee,Any,etsy,casual\n" +
                "Shirt,Cotton,L,Men,Wear,Summer,etsy,fun\n";

        BulkJobStatusDTO jobDto = bulkJobService.uploadAndQueue(createCsvFile(csv), testUser);
        BulkJob job = bulkJobRepository.findById(jobDto.jobId()).orElseThrow();

        List<BulkJobItem> items = bulkJobItemRepository.findByJobId(job.getId());
        
        // Mock success for first, failure for second
        EtsyOutputDTO mockEtsy = new EtsyOutputDTO(List.of("Title"), "Desc", List.of(), 100, List.of("tip"));
        
        when(claudeService.generateEtsy(any()))
            .thenReturn(CompletableFuture.completedFuture(mockEtsy)) // first call succeeds
            .thenThrow(new RuntimeException("Simulated error")); // second fails

        // Process both items
        bulkJobProcessor.processSingleItem(items.get(0));
        bulkJobProcessor.processSingleItem(items.get(1));

        BulkJobItem updatedItem1 = bulkJobItemRepository.findById(items.get(0).getId()).orElseThrow();
        BulkJobItem updatedItem2 = bulkJobItemRepository.findById(items.get(1).getId()).orElseThrow();

        assertEquals("COMPLETED", updatedItem1.getStatus());
        assertEquals("DEAD_LETTER", updatedItem2.getStatus());
    }
}
