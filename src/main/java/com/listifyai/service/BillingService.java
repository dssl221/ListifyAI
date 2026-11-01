package com.listifyai.service;

import com.listifyai.entity.ProcessedStripeEvent;
import com.listifyai.entity.User;
import com.listifyai.repository.ProcessedStripeEventRepository;
import com.listifyai.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@SuppressWarnings({"null", "unchecked", "unused"})
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${stripe.price.starter}")
    private String priceStarter;

    @Value("${stripe.price.growth}")
    private String priceGrowth;

    @Value("${stripe.price.pro}")
    private String pricePro;

    private final UserRepository userRepository;
    private final ProcessedStripeEventRepository processedStripeEventRepository;
    private final AuditService auditService;

    public BillingService(UserRepository userRepository,
                          ProcessedStripeEventRepository processedStripeEventRepository,
                          AuditService auditService) {
        this.userRepository = userRepository;
        this.processedStripeEventRepository = processedStripeEventRepository;
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public String createCheckoutSession(User user, String plan) throws StripeException {
        String priceId = resolvePriceId(plan);
        if (hasActiveStripeSubscription(user)) {
            throw new IllegalStateException("An active subscription already exists. Use the billing portal to change your plan.");
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(user.getEmail())
                .setSuccessUrl(frontendUrl + "/settings?success=true")
                .setCancelUrl(frontendUrl + "/pricing")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build())
                .putMetadata("userId", user.getId().toString())
                .build();

        // Stripe persists an idempotency key for 24 hours. This closes the
        // double-click/retry window before the first webhook has populated the
        // local subscription ID; after that, the active-subscription guard
        // prevents a second checkout.
        RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey("checkout:" + user.getId() + ":" + plan.toLowerCase())
                .build();
        Session session = Session.create(params, requestOptions);
        return session.getUrl();
    }

    private boolean hasActiveStripeSubscription(User user) {
        if (user.getStripeSubscriptionId() == null || user.getStripeSubscriptionId().isBlank()) {
            return false;
        }
        String status = user.getBillingStatus();
        return "active".equalsIgnoreCase(status)
                || "trialing".equalsIgnoreCase(status)
                || "past_due".equalsIgnoreCase(status)
                || "unpaid".equalsIgnoreCase(status);
    }

    public String createPortalSession(User user) throws StripeException {
        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank()) {
            throw new IllegalStateException("User does not have a Stripe customer ID");
        }

        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(user.getStripeCustomerId())
                        .setReturnUrl(frontendUrl + "/settings")
                        .build();

        com.stripe.model.billingportal.Session session =
                com.stripe.model.billingportal.Session.create(params);

        return session.getUrl();
    }

    @Transactional(rollbackFor = StripeException.class)
    public void handleWebhook(String payload, String sigHeader) throws StripeException {
        Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        if (processedStripeEventRepository.existsById(event.getId())) {
            return;
        }
        processedStripeEventRepository.save(new ProcessedStripeEvent(event.getId(), event.getType()));

        log.info("Processing Stripe webhook: id={}, type={}, livemode={}", event.getId(), event.getType(), event.getLivemode());

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                Session session = deserialize(event, Session.class);
                String userId = session.getMetadata().get("userId");

                if (userId != null) {
                    Subscription subscription = Subscription.retrieve(session.getSubscription());
                    String priceId = extractPriceId(subscription);
                    String plan = resolvePlanFromPriceId(priceId);

                    User user = userRepository.findById(java.util.UUID.fromString(userId)).orElse(null);
                    if (user == null) {
                        log.error("Webhook event {} received for unknown user ID: {}", event.getType(), userId);
                        break;
                    }

                    String oldPlan = user.getPlan();
                    user.setStripeCustomerId(session.getCustomer());
                    user.setStripeSubscriptionId(session.getSubscription());
                    user.setPlan(plan);
                    user.setBillingCycleStart(LocalDate.now());
                    
                    user.setBillingStatus(subscription.getStatus());
                    user.setCurrentPeriodEnd(OffsetDateTime.ofInstant(Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), ZoneId.of("UTC")));
                    user.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());

                    userRepository.save(user);
                    auditService.log(user, "billing.plan_changed", "User", user.getId().toString(), Map.of("plan", oldPlan), Map.of("plan", plan));
                }
            }

            case "customer.subscription.deleted" -> {
                Subscription subscription = deserialize(event, Subscription.class);
                User user = userRepository.findByStripeCustomerId(subscription.getCustomer()).orElse(null);
                if (user == null) {
                    log.error("Webhook event {} received for unknown customer ID: {}", event.getType(), subscription.getCustomer());
                    break;
                }
                String oldPlan = user.getPlan();
                user.setPlan("free");
                user.setBillingStatus(subscription.getStatus());
                user.setCurrentPeriodEnd(OffsetDateTime.ofInstant(Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), ZoneId.of("UTC")));
                user.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());
                userRepository.save(user);
                auditService.log(user, "billing.plan_changed", "User", user.getId().toString(), Map.of("plan", oldPlan), Map.of("plan", "free"));
            }

            case "customer.subscription.updated" -> {
                Subscription subscription = deserialize(event, Subscription.class);
                String customerId = subscription.getCustomer();
                String priceId = extractPriceId(subscription);
                String plan = resolvePlanFromPriceId(priceId);

                User user = userRepository.findByStripeCustomerId(customerId).orElse(null);
                if (user == null) {
                    log.error("Webhook event {} received for unknown customer ID: {}", event.getType(), customerId);
                    break;
                }
                String oldPlan = user.getPlan();
                user.setPlan(plan);
                user.setBillingCycleStart(LocalDate.now());
                
                user.setBillingStatus(subscription.getStatus());
                user.setCurrentPeriodEnd(OffsetDateTime.ofInstant(Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()), ZoneId.of("UTC")));
                user.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());

                userRepository.save(user);
                auditService.log(user, "billing.plan_changed", "User", user.getId().toString(), Map.of("plan", oldPlan), Map.of("plan", plan));
            }

            case "invoice.payment_failed" -> {
                com.stripe.model.Invoice invoice = deserialize(event, com.stripe.model.Invoice.class);
                String customerId = invoice.getCustomer();
                User user = userRepository.findByStripeCustomerId(customerId).orElse(null);
                if (user == null) {
                    log.error("Webhook event {} received for unknown customer ID: {}", event.getType(), customerId);
                    break;
                }
                user.setBillingStatus("past_due");
                userRepository.save(user);
                auditService.log(user, "billing.payment_failed", "User", user.getId().toString(), Map.of("invoiceId", invoice.getId()), null);
            }

            case "customer.deleted" -> {
                com.stripe.model.Customer customer = deserialize(event, com.stripe.model.Customer.class);
                User user = userRepository.findByStripeCustomerId(customer.getId()).orElse(null);
                if (user == null) {
                    log.error("Webhook event {} received for unknown customer ID: {}", event.getType(), customer.getId());
                    break;
                }
                String oldPlan = user.getPlan();
                user.setPlan("free");
                user.setStripeCustomerId(null);
                user.setStripeSubscriptionId(null);
                user.setBillingStatus(null);
                user.setCurrentPeriodEnd(null);
                user.setCancelAtPeriodEnd(false);
                userRepository.save(user);
                auditService.log(user, "billing.plan_changed", "User", user.getId().toString(), Map.of("plan", oldPlan, "reason", "customer.deleted"), Map.of("plan", "free"));
            }

            default -> {
                log.info("Unhandled Stripe event type: {}", event.getType());
            }
        }
    }

    private String resolvePriceId(String plan) {
        return switch (plan.toLowerCase()) {
            case "starter" -> priceStarter;
            case "growth" -> priceGrowth;
            case "pro" -> pricePro;
            default -> throw new IllegalArgumentException(
                    "Unknown plan: '" + plan + "'. Must be starter, growth, or pro.");
        };
    }

    private String resolvePlanFromPriceId(String priceId) {
        if (priceStarter.equals(priceId)) {
            return "starter";
        }
        if (priceGrowth.equals(priceId)) {
            return "growth";
        }
        if (pricePro.equals(priceId)) {
            return "pro";
        }
        return "free";
    }

    private String extractPriceId(Subscription subscription) {
        return subscription.getItems()
                .getData()
                .stream()
                .findFirst()
                .map(SubscriptionItem::getPrice)
                .map(com.stripe.model.Price::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "No price found on subscription: " + subscription.getId()));
    }

    @SuppressWarnings("unchecked")
    private <T extends com.stripe.model.StripeObject> T deserialize(Event event, Class<T> type) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        return (T) deserializer.getObject()
                .orElseThrow(() -> new IllegalStateException(
                        "Could not deserialize Stripe event object for: " + event.getType()));
    }
}
