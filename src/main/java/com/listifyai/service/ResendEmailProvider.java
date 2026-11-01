package com.listifyai.service;
import com.resend.Resend; import com.resend.core.exception.ResendException; import com.resend.services.emails.model.*;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Component;
@Component
public class ResendEmailProvider implements EmailProvider {
    private final String apiKey; private final String from;
    public ResendEmailProvider(@Value("${resend.api.key:}") String apiKey, @Value("${resend.from.email}") String from){this.apiKey=apiKey;this.from=from;}
    public String send(String recipient, String subject, String html) {
        if (apiKey.isBlank()) throw new EmailDeliveryException("Email provider is not configured", false, null);
        try { SendEmailResponse response = new Resend(apiKey).emails().send(SendEmailRequest.builder().from(from).to(recipient).subject(subject).html(html).build()); return response.getId(); }
        catch (ResendException ex) { String m=ex.getMessage()==null?"provider error":ex.getMessage(); boolean retryable=!(m.contains("400")||m.contains("401")||m.contains("403")||m.contains("422")); throw new EmailDeliveryException(m,retryable,ex); }
    }
}
