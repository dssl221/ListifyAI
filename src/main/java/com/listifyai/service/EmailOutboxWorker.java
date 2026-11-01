package com.listifyai.service;
import com.listifyai.entity.EmailOutbox; import com.listifyai.repository.EmailOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry; import org.slf4j.*; import org.springframework.beans.factory.annotation.Qualifier; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component; import org.springframework.transaction.support.TransactionTemplate;
import java.time.*; import java.util.*; import java.util.concurrent.Executor; import java.util.concurrent.ThreadLocalRandom;
@Component
public class EmailOutboxWorker {
    private static final Logger log=LoggerFactory.getLogger(EmailOutboxWorker.class); private final String workerId=UUID.randomUUID().toString();
    private final EmailOutboxRepository repository; private final EmailProvider provider; private final TransactionTemplate tx; private final Executor executor; private final MeterRegistry metrics;
    public EmailOutboxWorker(EmailOutboxRepository repository, EmailProvider provider, TransactionTemplate tx, @Qualifier("emailOutboxExecutor") Executor executor, MeterRegistry metrics){this.repository=repository;this.provider=provider;this.tx=tx;this.executor=executor;this.metrics=metrics;}
    @Scheduled(fixedDelayString="${email.outbox.poll-interval-ms:5000}") public void poll(){
        tx.executeWithoutResult(s->{int recovered=repository.recoverStuck(); if(recovered>0) metrics.counter("listifyai.email.outbox.recovered").increment(recovered);});
        List<EmailOutbox> messages=tx.execute(s->repository.claimDue(workerId, 20)); if(messages==null) return;
        messages.forEach(message -> executor.execute(()->deliver(message.getId())));
    }
    void deliver(UUID id){ tx.executeWithoutResult(s->{ EmailOutbox m=repository.findById(id).orElse(null); if(m==null||!"PROCESSING".equals(m.getStatus()))return; long started=System.nanoTime();
        try { String providerId=provider.send(m.getRecipient(),m.getSubject(),m.getHtmlBody()); m.setStatus("SENT");m.setSentAt(OffsetDateTime.now());m.setProviderMessageId(providerId);m.setLockedAt(null);m.setLockedBy(null);m.setLastError(null);metrics.counter("listifyai.email.outbox.sent","type",m.getMessageType()).increment(); }
        catch(EmailDeliveryException ex){fail(m,ex.isRetryable(),ex.getMessage());}
        catch(Exception ex){fail(m,true,ex.getClass().getSimpleName());}
        finally {metrics.timer("listifyai.email.provider.duration").record(Duration.ofNanos(System.nanoTime()-started)); repository.save(m);}
    }); }
    private void fail(EmailOutbox m, boolean retryable, String error){m.setLastError(error==null?"provider failure":error.substring(0,Math.min(2000,error.length())));m.setLockedAt(null);m.setLockedBy(null);
        if(!retryable||m.getAttempts()>=m.getMaxAttempts()){m.setStatus("DEAD");metrics.counter("listifyai.email.outbox.dead","type",m.getMessageType()).increment();log.warn("Email outbox message {} moved to DEAD after {} attempts",m.getId(),m.getAttempts());return;}
        long seconds=Math.min(3600, 30L*(1L << Math.min(6,m.getAttempts()-1)))+ThreadLocalRandom.current().nextLong(0,15);m.setStatus("RETRYING");m.setNextAttemptAt(OffsetDateTime.now().plusSeconds(seconds));metrics.counter("listifyai.email.outbox.retry","type",m.getMessageType()).increment(); }
}
