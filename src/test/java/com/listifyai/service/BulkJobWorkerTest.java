package com.listifyai.service;

import com.listifyai.entity.BulkJob;
import com.listifyai.entity.BulkJobItem;
import com.listifyai.repository.BulkJobItemRepository;
import com.listifyai.repository.BulkJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkJobWorkerTest {

    @Mock private BulkJobItemRepository itemRepository;
    @Mock private BulkJobRepository jobRepository;
    @Mock private BulkJobProcessor processor;
    @Mock private BulkJobCompletionService completionService;

    @Test
    void claimsAndProcessesPendingItems() {
        BulkJob job = new BulkJob();
        BulkJobItem item = new BulkJobItem();
        item.setJob(job);
        when(itemRepository.claimNextItems(anyString(), eq(10))).thenReturn(List.of(item));

        BulkJobWorker worker = new BulkJobWorker(
                itemRepository, jobRepository, processor, completionService, Runnable::run);

        worker.pollAndProcess();

        verify(processor).processSingleItem(item);
        verify(completionService).checkAndCompleteJob(job.getId());
    }
}
