package com.listifyai.controller;

import com.listifyai.entity.User;
import com.listifyai.security.AuthUtils;
import com.listifyai.service.BillingService;
import com.stripe.exception.StripeException;
import com.stripe.exception.SignatureVerificationException;
import io.micrometer.core.instrument.MeterRegistry;
import io.sentry.Sentry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;
    private final MeterRegistry meterRegistry;

    public BillingController(BillingService billingService, MeterRegistry meterRegistry) {
        this.billingService = billingService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * POST /api/billing/checkout?plan=starter
     * Requires authentication. Creates a Stripe Checkout session for the
     * given plan and returns the hosted URL for the frontend to redirect to.
     */
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> checkout(
            @RequestParam String plan) throws StripeException {

        User user = AuthUtils.getCurrentUser();
        AuthUtils.assertNotSuspended(user);
        String checkoutUrl = billingService.createCheckoutSession(user, plan);
        return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    }

    /**
     * POST /api/billing/portal
     * Creates a Stripe Customer Portal session.
     */
    @PostMapping("/portal")
    public ResponseEntity<Map<String, String>> portal() throws StripeException {
        User user = AuthUtils.getCurrentUser();
        AuthUtils.assertNotSuspended(user);
        String portalUrl = billingService.createPortalSession(user);
        return ResponseEntity.ok(Map.of("portalUrl", portalUrl));
    }

    /**
     * POST /api/billing/webhook
     * Public endpoint — no JWT auth (Stripe calls this directly).
     * Stripe-Signature header is used for payload verification inside BillingService.
     * Must be listed as permitAll() in SecurityConfig.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) throws StripeException {
        
        meterRegistry.counter("listifyai.stripe.webhook", "status", "received").increment();
        try {
            billingService.handleWebhook(payload, sigHeader);
            meterRegistry.counter("listifyai.stripe.webhook", "status", "processed").increment();
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            meterRegistry.counter("listifyai.stripe.webhook", "status", "failed").increment();
            Sentry.withScope(scope -> {
                scope.setTag("feature_area", "billing");
                Sentry.captureException(ex);
            });
            throw ex;
        }
    }

    @ExceptionHandler(SignatureVerificationException.class)
    public ResponseEntity<String> handleSignatureException(SignatureVerificationException e) {
        meterRegistry.counter("listifyai.stripe.webhook", "status", "failed").increment();
        return ResponseEntity.badRequest().body("Invalid Stripe signature");
    }
}
