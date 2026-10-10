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
public class DecisionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId;

    private Long employeeId;

    @Enumerated(EnumType.STRING)
    private Decision decision;

    private String reasons;

    @Column(length = 4000)
    private String trace;

    private long configVersion;

    private LocalDateTime evaluatedAt;

    protected DecisionLog() {
    }

    public DecisionLog(String requestId, Long employeeId, Decision decision, String reasons,
                       String trace, long configVersion, LocalDateTime evaluatedAt) {
        this.requestId = requestId;
        this.employeeId = employeeId;
        this.decision = decision;
        this.reasons = reasons;
        this.trace = trace;
        this.configVersion = configVersion;
        this.evaluatedAt = evaluatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getReasons() {
        return reasons;
    }

    public String getTrace() {
        return trace;
    }

    public long getConfigVersion() {
        return configVersion;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }
}
