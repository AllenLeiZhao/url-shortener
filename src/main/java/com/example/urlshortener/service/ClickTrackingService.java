package com.example.urlshortener.service;

import com.example.urlshortener.model.ClickEvent;
import com.example.urlshortener.repository.ClickEventRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ClickTrackingService {

    private static final Logger log = LoggerFactory.getLogger(ClickTrackingService.class);

    private final ClickEventRepository repository;
    private final Clock clock;

    public ClickTrackingService(ClickEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Fire-and-forget: runs on the analytics executor, never on the redirect thread.
     * A failed write is logged and swallowed — losing a click must not lose the redirect.
     */
    @Async("analyticsExecutor")
    public void record(String code, String referrer, String userAgent) {
        try {
            repository.save(new ClickEvent(
                    code,
                    Instant.now(clock),
                    truncate(referrer, ClickEvent.MAX_REFERRER_LENGTH),
                    truncate(userAgent, ClickEvent.MAX_USER_AGENT_LENGTH)));
        } catch (Exception e) {
            log.warn("Failed to record click for code {}: {}", code, e.getMessage());
        }
    }

    public UrlStats statsFor(String code) {
        Instant now = Instant.now(clock);
        return new UrlStats(
                repository.countByCode(code),
                repository.countByCodeAndOccurredAtAfter(code, now.minus(Duration.ofHours(24))),
                repository
                        .findTopByCodeOrderByOccurredAtDesc(code)
                        .map(ClickEvent::getOccurredAt)
                        .orElse(null));
    }

    public record UrlStats(long totalClicks, long clicksLast24h, Instant lastClickAt) {}

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
