package com.example.bookingengine.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class RuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientId;

    private Long costCentreId;

    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    private boolean enabled;

    private int priority;

    @Enumerated(EnumType.STRING)
    private Decision onFail;

    @Column(length = 1000)
    private String params;

    private LocalDateTime updatedAt;

    protected RuleConfig() {
    }

    public RuleConfig(String clientId, Long costCentreId, RuleType ruleType, boolean enabled,
                      int priority, Decision onFail, String params) {
        this.clientId = clientId;
        this.costCentreId = costCentreId;
        this.ruleType = ruleType;
        this.enabled = enabled;
        this.priority = priority;
        this.onFail = onFail;
        this.params = params;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public Long getCostCentreId() {
        return costCentreId;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Decision getOnFail() {
        return onFail;
    }

    public void setOnFail(Decision onFail) {
        this.onFail = onFail;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
