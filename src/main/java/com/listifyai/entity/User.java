package com.listifyai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "name_prompt_completed", nullable = false)
    private Boolean namePromptCompleted = true;

    @Column(name = "supabase_id", unique = true, nullable = false, length = 255)
    private String supabaseId;

    @Column(name = "plan", nullable = false, length = 20)
    private String plan = "free";

    @Column(name = "role", nullable = false, length = 20)
    private String role = "user";

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 100)
    private String stripeSubscriptionId;

    @Column(name = "billing_status", length = 50)
    private String billingStatus;

    @Column(name = "current_period_end")
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "cancel_at_period_end")
    private Boolean cancelAtPeriodEnd = false;

    @Column(name = "billing_cycle_start")
    private LocalDate billingCycleStart;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public User() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Boolean getNamePromptCompleted() { return namePromptCompleted; }
    public void setNamePromptCompleted(Boolean namePromptCompleted) { this.namePromptCompleted = namePromptCompleted; }
    
    public String getSupabaseId() { return supabaseId; }
    public void setSupabaseId(String supabaseId) { this.supabaseId = supabaseId; }
    
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
    
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }

    public String getBillingStatus() { return billingStatus; }
    public void setBillingStatus(String billingStatus) { this.billingStatus = billingStatus; }

    public OffsetDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(OffsetDateTime currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }

    public Boolean getCancelAtPeriodEnd() { return cancelAtPeriodEnd; }
    public void setCancelAtPeriodEnd(Boolean cancelAtPeriodEnd) { this.cancelAtPeriodEnd = cancelAtPeriodEnd; }
    
    public LocalDate getBillingCycleStart() { return billingCycleStart; }
    public void setBillingCycleStart(LocalDate billingCycleStart) { this.billingCycleStart = billingCycleStart; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
