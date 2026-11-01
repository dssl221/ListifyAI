package com.listifyai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.dto.ProductInputDTO;
import com.listifyai.entity.BulkJob;
import com.listifyai.entity.BulkJobItem;
import com.listifyai.entity.User;
import com.listifyai.exception.ListingLimitException;
import com.listifyai.repository.BulkJobItemRepository;
import com.listifyai.repository.BulkJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkJobServiceTest {

    @Mock
    private BulkJobRepository bulkJobRepository;

    @Mock
    private BulkJobItemRepository bulkJobItemRepository;

    @Mock
    private UsageService usageService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private BulkJobService bulkJobService;

    @Captor
    private ArgumentCaptor<List<BulkJobItem>> itemsCaptor;

    @Test
    void uploadAndQueueThrows402WhenPlanIsNotPro() {
        MockMultipartFile file = csv("""
                productName,material,size,targetBuyer,useCase,occasion,platform,tone
                Mug,Ceramic,12 oz,Teachers,Coffee,Birthday,Etsy,Playful
                """);

        assertThrows(ListingLimitException.class, () -> bulkJobService.uploadAndQueue(file, user("growth")));

        verifyNoInteractions(bulkJobRepository, bulkJobItemRepository);
    }

    @Test
    void uploadAndQueueThrows400WhenFileExceeds2Mb() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(2L * 1024 * 1024 + 1);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bulkJobService.uploadAndQueue(file, user("pro"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(bulkJobRepository, bulkJobItemRepository);
    }

    @Test
    void uploadAndQueueThrows400WhenCsvHasMissingColumns() {
        MockMultipartFile file = csv("""
                productName,material,size,targetBuyer,useCase,occasion,platform
                Mug,Ceramic,12 oz,Teachers,Coffee,Birthday,Etsy
                """);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bulkJobService.uploadAndQueue(file, user("pro"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(bulkJobRepository, bulkJobItemRepository);
    }

    @Test
    void uploadAndQueueThrows400WhenCsvHasZeroDataRows() {
        MockMultipartFile file = csv("""
                productName,material,size,targetBuyer,useCase,occasion,platform,tone
                """);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bulkJobService.uploadAndQueue(file, user("pro"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(bulkJobRepository, bulkJobItemRepository);
    }

    @Test
    void uploadAndQueueThrows400WhenCsvExceeds50Rows() {
        String rows = IntStream.rangeClosed(1, 51)
                .mapToObj(i -> "Mug " + i + ",Ceramic,12 oz,Teachers,Coffee,Birthday,Etsy,Playful")
                .reduce("", (left, right) -> left + right + "\n");
        MockMultipartFile file = csv("""
                productName,material,size,targetBuyer,useCase,occasion,platform,tone
                """ + rows);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bulkJobService.uploadAndQueue(file, user("pro"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(bulkJobRepository, bulkJobItemRepository);
    }

    @Test
    void uploadAndQueueThrows402WhenRowsExceedRemainingCredits() {
        MockMultipartFile file = csv("""
                productName,material,size,targetBuyer,useCase,occasion,platform,tone
                Mug 1,Ceramic,12 oz,Teachers,Coffee,Birthday,Etsy,Playful
                Mug 2,Ceramic,12 oz,Teachers,Coffee,Birthday,Etsy,Playful
                Mug 3,Ceramic,12 oz,Teachers,Coffee,Birthday,Etsy,Playful
                """);
        User user = user("pro");
        when(usageService.getRemainingCredits(user)).thenReturn("2");

        ListingLimitException exception = assertThrows(
                ListingLimitException.class,
                () -> bulkJobService.uploadAndQueue(file, user)
        );

        verifyNoInteractions(bulkJobRepository, bulkJobItemRepository);
    }

    @Test
    void uploadAndQueueSuccessfullyPersistsJobAndItems() throws IOException {
        MockMultipartFile file = csv("""
                productName,material,size,targetBuyer,useCase,occasion,platform,tone
                Travel Mug,Stainless steel,16 oz,Commuters,Morning coffee,Holiday,Amazon,Confident
                """);
        User user = user("pro");
        when(usageService.getRemainingCredits(user)).thenReturn("Unlimited");

        BulkJob savedJob = new BulkJob();
        savedJob.setId(UUID.randomUUID());
        savedJob.setTotalItems(1);
        savedJob.setCompletedItems(0);
        savedJob.setFailedItems(0);
        when(bulkJobRepository.save(any(BulkJob.class))).thenReturn(savedJob);

        bulkJobService.uploadAndQueue(file, user);

        verify(bulkJobRepository).save(any(BulkJob.class));
        verify(bulkJobItemRepository).saveAll(itemsCaptor.capture());

        List<BulkJobItem> items = itemsCaptor.getValue();
        assertEquals(1, items.size());
        assertEquals("PENDING", items.get(0).getStatus());
    }

    @Test
    void parseCsvCorrectlyMapsAll8ColumnsToProductInputDto() throws IOException {
        MockMultipartFile file = csv("""
                productName,material,size,targetBuyer,useCase,occasion,platform,tone
                Travel Mug,Stainless steel,16 oz,Commuters,Morning coffee,Holiday,Amazon,Confident
                """);

        List<ProductInputDTO> rows = bulkJobService.parseCsv(file);

        assertEquals(1, rows.size());
        ProductInputDTO row = rows.get(0);
        assertEquals("Travel Mug", row.productName());
        assertEquals("Stainless steel", row.material());
        assertEquals("16 oz", row.size());
        assertEquals("Commuters", row.targetBuyer());
        assertEquals("Morning coffee", row.useCase());
        assertEquals("Holiday", row.occasion());
        assertEquals("Amazon", row.platform());
        assertEquals("Confident", row.tone());
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile(
                "file",
                "products.csv",
                "text/csv",
                content.getBytes()
        );
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
