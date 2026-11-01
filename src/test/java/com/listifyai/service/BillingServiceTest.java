package com.listifyai.service;

import com.listifyai.entity.User;
import com.listifyai.repository.ProcessedStripeEventRepository;
import com.listifyai.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked", "unused"})
public class BillingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProcessedStripeEventRepository processedStripeEventRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(billingService, "webhookSecret", "whsec_test");
        ReflectionTestUtils.setField(billingService, "priceStarter", "price_starter");
        ReflectionTestUtils.setField(billingService, "priceGrowth", "price_growth");
        ReflectionTestUtils.setField(billingService, "pricePro", "price_pro");
    }

    @Test
    void testWebhook_InvalidSignature_ThrowsException() {
        assertThrows(SignatureVerificationException.class, () -> {
            billingService.handleWebhook("{}", "invalid_sig");
        });
    }

    @Test
    void testWebhook_DuplicateEvent_Ignored() throws StripeException {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_123");
        
        when(processedStripeEventRepository.existsById("evt_123")).thenReturn(true);

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("{}", "sig", "whsec_test"))
                    .thenReturn(mockEvent);

            billingService.handleWebhook("{}", "sig");

            verify(processedStripeEventRepository, never()).save(any());
            verify(userRepository, never()).findById(any(UUID.class));
        }
    }

    @Test
    void testWebhook_UnknownUser_LogsErrorAndDoesNotCrash() throws StripeException {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_123");
        when(mockEvent.getType()).thenReturn("customer.subscription.deleted");
        when(mockEvent.getLivemode()).thenReturn(false);

        Subscription mockSubscription = mock(Subscription.class);
        when(mockSubscription.getCustomer()).thenReturn("cus_unknown");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(mockSubscription));

        when(processedStripeEventRepository.existsById("evt_123")).thenReturn(false);
        when(userRepository.findByStripeCustomerId("cus_unknown")).thenReturn(Optional.empty());

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("{}", "sig", "whsec_test"))
                    .thenReturn(mockEvent);

            assertDoesNotThrow(() -> billingService.handleWebhook("{}", "sig"));

            verify(processedStripeEventRepository).save(any());
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void testWebhook_CustomerSubscriptionDeleted_DowngradesUser() throws StripeException {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_123");
        when(mockEvent.getType()).thenReturn("customer.subscription.deleted");
        when(mockEvent.getLivemode()).thenReturn(false);

        Subscription mockSubscription = mock(Subscription.class);
        when(mockSubscription.getCustomer()).thenReturn("cus_123");
        when(mockSubscription.getStatus()).thenReturn("canceled");
        when(mockSubscription.getCurrentPeriodEnd()).thenReturn(1600000000L);
        when(mockSubscription.getCancelAtPeriodEnd()).thenReturn(false);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(mockSubscription));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPlan("pro");
        
        when(processedStripeEventRepository.existsById("evt_123")).thenReturn(false);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("payload", "sig", "whsec_test"))
                    .thenReturn(mockEvent);

            billingService.handleWebhook("payload", "sig");

            verify(processedStripeEventRepository).save(any());
            verify(userRepository).save(user);
            assertEquals("free", user.getPlan());
            assertEquals("canceled", user.getBillingStatus());
            assertNotNull(user.getCurrentPeriodEnd());
        }
    }

    @Test
    void testWebhook_CheckoutSessionCompleted_UpgradesPlan() throws StripeException {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_chk");
        when(mockEvent.getType()).thenReturn("checkout.session.completed");
        when(mockEvent.getLivemode()).thenReturn(false);

        Session mockSession = mock(Session.class);
        when(mockSession.getSubscription()).thenReturn("sub_123");
        when(mockSession.getCustomer()).thenReturn("cus_123");
        when(mockSession.getMetadata()).thenReturn(java.util.Map.of("userId", "123e4567-e89b-12d3-a456-426614174000"));

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(mockSession));

        Subscription mockSubscription = mock(Subscription.class);
        when(mockSubscription.getStatus()).thenReturn("active");
        when(mockSubscription.getCurrentPeriodEnd()).thenReturn(1600000000L);
        when(mockSubscription.getCancelAtPeriodEnd()).thenReturn(false);

        SubscriptionItem mockItem = mock(SubscriptionItem.class);
        Price mockPrice = mock(Price.class);
        when(mockPrice.getId()).thenReturn("price_pro");
        when(mockItem.getPrice()).thenReturn(mockPrice);

        SubscriptionItemCollection itemCollection = mock(SubscriptionItemCollection.class);
        when(itemCollection.getData()).thenReturn(java.util.List.of(mockItem));
        when(mockSubscription.getItems()).thenReturn(itemCollection);

        User user = new User();
        user.setId(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        user.setPlan("free");

        when(processedStripeEventRepository.existsById("evt_chk")).thenReturn(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class);
             MockedStatic<Subscription> mockedSubscription = mockStatic(Subscription.class)) {
            
            mockedWebhook.when(() -> Webhook.constructEvent("payload", "sig", "whsec_test"))
                    .thenReturn(mockEvent);
            mockedSubscription.when(() -> Subscription.retrieve("sub_123"))
                    .thenReturn(mockSubscription);

            billingService.handleWebhook("payload", "sig");

            verify(processedStripeEventRepository).save(any());
            verify(userRepository).save(user);
            assertEquals("pro", user.getPlan());
            assertEquals("cus_123", user.getStripeCustomerId());
            assertEquals("sub_123", user.getStripeSubscriptionId());
            assertEquals("active", user.getBillingStatus());
            assertNotNull(user.getCurrentPeriodEnd());
        }
    }

    @Test
    void testWebhook_CustomerSubscriptionUpdated_ChangesPlan() throws StripeException {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_upd");
        when(mockEvent.getType()).thenReturn("customer.subscription.updated");
        when(mockEvent.getLivemode()).thenReturn(false);

        Subscription mockSubscription = mock(Subscription.class);
        when(mockSubscription.getCustomer()).thenReturn("cus_123");
        when(mockSubscription.getStatus()).thenReturn("active");
        when(mockSubscription.getCurrentPeriodEnd()).thenReturn(1600000000L);
        when(mockSubscription.getCancelAtPeriodEnd()).thenReturn(false);

        SubscriptionItem mockItem = mock(SubscriptionItem.class);
        Price mockPrice = mock(Price.class);
        when(mockPrice.getId()).thenReturn("price_growth");
        when(mockItem.getPrice()).thenReturn(mockPrice);

        SubscriptionItemCollection itemCollection = mock(SubscriptionItemCollection.class);
        when(itemCollection.getData()).thenReturn(java.util.List.of(mockItem));
        when(mockSubscription.getItems()).thenReturn(itemCollection);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(mockSubscription));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPlan("pro");
        user.setStripeCustomerId("cus_123");

        when(processedStripeEventRepository.existsById("evt_upd")).thenReturn(false);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent("payload", "sig", "whsec_test"))
                    .thenReturn(mockEvent);

            billingService.handleWebhook("payload", "sig");

            verify(processedStripeEventRepository).save(any());
            verify(userRepository).save(user);
            assertEquals("growth", user.getPlan());
            assertEquals("active", user.getBillingStatus());
        }
    }
}
