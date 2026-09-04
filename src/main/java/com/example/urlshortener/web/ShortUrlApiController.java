package com.example.urlshortener.web;

import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.service.ClickTrackingService;
import com.example.urlshortener.service.UrlShortenerService;
import com.example.urlshortener.web.dto.CreateUrlRequest;
import com.example.urlshortener.web.dto.ShortUrlResponse;
import com.example.urlshortener.web.dto.UrlStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/urls")
@Tag(name = "Short URLs", description = "Create and inspect short URLs")
public class ShortUrlApiController {

    private final UrlShortenerService service;
    private final ClickTrackingService clickTracking;

    public ShortUrlApiController(UrlShortenerService service, ClickTrackingService clickTracking) {
        this.service = service;
        this.clickTracking = clickTracking;
    }

    @PostMapping
    @Operation(summary = "Shorten a URL")
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        ShortUrl shortUrl = service.shorten(request.url());
        ShortUrlResponse body = ShortUrlResponse.of(shortUrl, service.shortUrlFor(shortUrl));
        return ResponseEntity.created(URI.create(body.shortUrl())).body(body);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Look up a short URL's metadata without redirecting")
    public ShortUrlResponse get(@PathVariable String code) {
        ShortUrl shortUrl = service.resolve(code);
        return ShortUrlResponse.of(shortUrl, service.shortUrlFor(shortUrl));
    }

    @GetMapping("/{code}/stats")
    @Operation(summary = "Click statistics for a short URL")
    public UrlStatsResponse stats(@PathVariable String code) {
        service.resolve(code); // 404 for unknown codes before aggregating
        return UrlStatsResponse.of(code, clickTracking.statsFor(code));
    }
}
