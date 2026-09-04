package com.example.urlshortener.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One row per successful redirect. References the short code by value rather than
 * a FK to short_urls so the hot redirect path stays write-decoupled (ADR-005).
 * Deliberately no IP address or other PII — see brownfield-analytics.md privacy floor.
 */
@Entity
@Table(name = "click_events", indexes = @Index(name = "idx_click_events_code_time", columnList = "code, occurredAt"))
public class ClickEvent {

    public static final int MAX_REFERRER_LENGTH = 512;
    public static final int MAX_USER_AGENT_LENGTH = 256;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String code;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(length = MAX_REFERRER_LENGTH)
    private String referrer;

    @Column(length = MAX_USER_AGENT_LENGTH)
    private String userAgent;

    protected ClickEvent() {
        // JPA
    }

    public ClickEvent(String code, Instant occurredAt, String referrer, String userAgent) {
        this.code = code;
        this.occurredAt = occurredAt;
        this.referrer = referrer;
        this.userAgent = userAgent;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
