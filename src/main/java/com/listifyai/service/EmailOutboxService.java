package com.listifyai.service;
import com.listifyai.entity.EmailOutbox; import com.listifyai.repository.EmailOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry; import org.springframework.dao.DataIntegrityViolationException; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
@Service
public class EmailOutboxService {
    private final EmailOutboxRepository repository; private final MeterRegistry metrics;
    public EmailOutboxService(EmailOutboxRepository repository, MeterRegistry metrics){this.repository=repository;this.metrics=metrics;}
    @Transactional
    public EmailOutbox enqueue(String type, String recipient, String subject, String html, String key) {
        if (recipient == null || !recipient.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Invalid email recipient");
        try {
            EmailOutbox m=new EmailOutbox(); m.setMessageType(type); m.setRecipient(recipient); m.setSubject(subject); m.setHtmlBody(html); m.setIdempotencyKey(key); m.setNextAttemptAt(OffsetDateTime.now());
            EmailOutbox saved=repository.saveAndFlush(m); metrics.counter("listifyai.email.outbox.enqueued", "type", type).increment(); return saved;
        } catch (DataIntegrityViolationException ex) { return repository.findByIdempotencyKey(key).orElseThrow(()->ex); }
    }
}
