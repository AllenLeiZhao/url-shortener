package com.example.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.urlshortener.exception.CodeGenerationException;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.ShortUrlNotFoundException;
import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.repository.ShortUrlRepository;
import com.example.urlshortener.service.AppProperties;
import com.example.urlshortener.service.ShortCodeGenerator;
import com.example.urlshortener.service.UrlShortenerService;
import com.example.urlshortener.service.UrlValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-03T12:00:00Z");

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private ShortCodeGenerator codeGenerator;

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                "http://localhost:8080", new AppProperties.ShortCode(7, 5), new AppProperties.RateLimit(20));
        service = new UrlShortenerService(
                repository, codeGenerator, new UrlValidator(), props, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shortensValidUrl() {
        when(codeGenerator.generate(7)).thenReturn("Abc1234");
        when(repository.save(any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));

        ShortUrl result = service.shorten("https://example.com/some/long/path?q=1");

        assertThat(result.getCode()).isEqualTo("Abc1234");
        assertThat(result.getOriginalUrl()).isEqualTo("https://example.com/some/long/path?q=1");
        assertThat(result.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void retriesOnCodeCollision() {
        when(codeGenerator.generate(7)).thenReturn("dup0000", "fresh00");
        when(repository.save(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(inv -> inv.getArgument(0));

        ShortUrl result = service.shorten("https://example.com");

        assertThat(result.getCode()).isEqualTo("fresh00");
        verify(repository, times(2)).save(any(ShortUrl.class));
    }

    @Test
    void failsAfterExhaustingCollisionRetries() {
        when(codeGenerator.generate(7)).thenReturn("dup0000");
        when(repository.save(any(ShortUrl.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.shorten("https://example.com")).isInstanceOf(CodeGenerationException.class);
        verify(repository, times(5)).save(any(ShortUrl.class));
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertThatThrownBy(() -> service.shorten("javascript:alert(1)")).isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> service.shorten("file:///etc/passwd")).isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> service.shorten("ftp://example.com/file")).isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejectsMalformedOrHostlessUrls() {
        assertThatThrownBy(() -> service.shorten("not a url")).isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> service.shorten("http:noHost")).isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> service.shorten("   ")).isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejectsOversizedUrl() {
        String longUrl = "https://example.com/" + "a".repeat(UrlValidator.MAX_URL_LENGTH);
        assertThatThrownBy(() -> service.shorten(longUrl)).isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void resolveThrowsForUnknownCode() {
        when(repository.findByCode("nope123")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolve("nope123")).isInstanceOf(ShortUrlNotFoundException.class);
    }

    @Test
    void buildsShortUrlFromBase() {
        ShortUrl entity = new ShortUrl("Abc1234", "https://example.com", NOW);
        assertThat(service.shortUrlFor(entity)).isEqualTo("http://localhost:8080/Abc1234");
    }
}
