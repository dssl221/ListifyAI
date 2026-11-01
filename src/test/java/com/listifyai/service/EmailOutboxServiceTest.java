package com.listifyai.service;

import com.listifyai.entity.EmailOutbox;
import com.listifyai.repository.EmailOutboxRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailOutboxServiceTest {
    @Mock EmailOutboxRepository repository;
    @Test void enqueuesDurableMessageWithIdempotencyKey() {
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        EmailOutbox message = new EmailOutboxService(repository, new SimpleMeterRegistry())
                .enqueue("WEEKLY_SELLER_REPORT", "seller@example.com", "Report", "<p>ok</p>", "weekly-report:user:2026-08-17");
        assertEquals("PENDING", message.getStatus()); assertEquals("weekly-report:user:2026-08-17", message.getIdempotencyKey());
    }
    @Test void duplicateCreationReturnsOriginalMessage() {
        EmailOutbox existing = new EmailOutbox(); existing.setIdempotencyKey("monitoring-alert:1");
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));
        when(repository.findByIdempotencyKey("monitoring-alert:1")).thenReturn(Optional.of(existing));
        assertSame(existing, new EmailOutboxService(repository, new SimpleMeterRegistry()).enqueue("MONITORING_ALERT", "seller@example.com", "Alert", "<p>ok</p>", "monitoring-alert:1"));
    }
    @Test void rejectsMalformedRecipientBeforePersisting() {
        assertThrows(IllegalArgumentException.class, () -> new EmailOutboxService(repository, new SimpleMeterRegistry()).enqueue("X", "bad-address", "x", "x", "x"));
        verifyNoInteractions(repository);
    }
}
