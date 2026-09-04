package com.example.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.urlshortener.model.ClickEvent;
import com.example.urlshortener.repository.ClickEventRepository;
import com.example.urlshortener.service.ClickTrackingService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClickTrackingServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-03T12:00:00Z");

    @Mock
    private ClickEventRepository repository;

    private ClickTrackingService service;

    @BeforeEach
    void setUp() {
        service = new ClickTrackingService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsClickWithTruncatedMetadata() {
        String longReferrer = "https://ref.example/" + "r".repeat(600);
        String longUserAgent = "UA/" + "u".repeat(300);

        service.record("Abc1234", longReferrer, longUserAgent);

        ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(repository).save(captor.capture());
        ClickEvent saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("Abc1234");
        assertThat(saved.getOccurredAt()).isEqualTo(NOW);
        assertThat(saved.getReferrer()).hasSize(ClickEvent.MAX_REFERRER_LENGTH);
        assertThat(saved.getUserAgent()).hasSize(ClickEvent.MAX_USER_AGENT_LENGTH);
    }

    @Test
    void recordAllowsNullMetadata() {
        service.record("Abc1234", null, null);

        ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReferrer()).isNull();
        assertThat(captor.getValue().getUserAgent()).isNull();
    }

    @Test
    void recordSwallowsStorageFailures() {
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> service.record("Abc1234", null, null)).doesNotThrowAnyException();
    }

    @Test
    void aggregatesStats() {
        when(repository.countByCode("Abc1234")).thenReturn(10L);
        when(repository.countByCodeAndOccurredAtAfter(eq("Abc1234"), any(Instant.class)))
                .thenReturn(3L);
        when(repository.findTopByCodeOrderByOccurredAtDesc("Abc1234"))
                .thenReturn(Optional.of(new ClickEvent("Abc1234", NOW, null, null)));

        ClickTrackingService.UrlStats stats = service.statsFor("Abc1234");

        assertThat(stats.totalClicks()).isEqualTo(10L);
        assertThat(stats.clicksLast24h()).isEqualTo(3L);
        assertThat(stats.lastClickAt()).isEqualTo(NOW);
    }

    @Test
    void statsForNeverClickedCode() {
        when(repository.countByCode("fresh00")).thenReturn(0L);
        when(repository.countByCodeAndOccurredAtAfter(eq("fresh00"), any(Instant.class)))
                .thenReturn(0L);
        when(repository.findTopByCodeOrderByOccurredAtDesc("fresh00")).thenReturn(Optional.empty());

        ClickTrackingService.UrlStats stats = service.statsFor("fresh00");

        assertThat(stats.totalClicks()).isZero();
        assertThat(stats.lastClickAt()).isNull();
    }
}
