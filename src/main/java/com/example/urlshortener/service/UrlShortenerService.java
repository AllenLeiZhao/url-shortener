package com.example.urlshortener.service;

import com.example.urlshortener.exception.CodeGenerationException;
import com.example.urlshortener.exception.ShortUrlNotFoundException;
import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.repository.ShortUrlRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UrlShortenerService {

    private final ShortUrlRepository repository;
    private final ShortCodeGenerator codeGenerator;
    private final UrlValidator urlValidator;
    private final AppProperties properties;
    private final Clock clock;

    public UrlShortenerService(
            ShortUrlRepository repository,
            ShortCodeGenerator codeGenerator,
            UrlValidator urlValidator,
            AppProperties properties,
            Clock clock) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.urlValidator = urlValidator;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Collisions are handled by relying on the DB unique constraint rather than a
     * check-then-insert, which would race under concurrent requests.
     */
    public ShortUrl shorten(String rawUrl) {
        String url = urlValidator.validate(rawUrl);
        int maxRetries = properties.shortCode().maxCollisionRetries();
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            String code = codeGenerator.generate(properties.shortCode().length());
            try {
                return repository.save(new ShortUrl(code, url, Instant.now(clock)));
            } catch (DataIntegrityViolationException e) {
                // code collision — regenerate and retry
            }
        }
        throw new CodeGenerationException(maxRetries);
    }

    public ShortUrl resolve(String code) {
        return repository.findByCode(code).orElseThrow(() -> new ShortUrlNotFoundException(code));
    }

    public String shortUrlFor(ShortUrl shortUrl) {
        String base = properties.baseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/" + shortUrl.getCode();
    }
}
