package com.example.urlshortener.web;

import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.service.ClickTrackingService;
import com.example.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Redirect", description = "Short link resolution")
public class RedirectController {

    private final UrlShortenerService service;
    private final ClickTrackingService clickTracking;

    public RedirectController(UrlShortenerService service, ClickTrackingService clickTracking) {
        this.service = service;
        this.clickTracking = clickTracking;
    }

    /**
     * 302 rather than 301 so hits always reach the service — keeps links
     * revocable and analytics accurate (ADR-003). Click capture is async
     * fire-and-forget and adds no latency to the redirect.
     */
    @GetMapping("/{code:[0-9A-Za-z]{1,16}}")
    @Operation(summary = "Redirect to the original URL")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        ShortUrl shortUrl = service.resolve(code);
        clickTracking.record(code, request.getHeader(HttpHeaders.REFERER), request.getHeader(HttpHeaders.USER_AGENT));
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, shortUrl.getOriginalUrl())
                .build();
    }
}
