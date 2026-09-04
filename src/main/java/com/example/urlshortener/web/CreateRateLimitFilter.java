package com.example.urlshortener.web;

import com.example.urlshortener.service.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed-window per-IP rate limit on the anonymous write path only
 * (ambiguous-reliability.md R2). Reads and redirects are never limited.
 *
 * Uses the socket address, not X-Forwarded-For: with no trusted proxy in front,
 * honoring the header would let clients spoof their way around the limit.
 * Per-instance state by design; a multi-instance deployment moves this to Redis.
 */
@Component
public class CreateRateLimitFilter extends OncePerRequestFilter {

    private record Window(long epochMinute, AtomicInteger count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AppProperties properties;
    private final Clock clock;

    public CreateRateLimitFilter(AppProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/api/urls".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long nowMinute = Instant.now(clock).getEpochSecond() / 60;
        Window window = windows.compute(
                request.getRemoteAddr(),
                (ip, existing) -> existing == null || existing.epochMinute() != nowMinute
                        ? new Window(nowMinute, new AtomicInteger())
                        : existing);
        pruneStaleWindows(nowMinute);

        if (window.count().incrementAndGet() > properties.rateLimit().createPerMinute()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HttpHeaders.RETRY_AFTER, "60");
            response.getWriter()
                    .write(
                            """
                    {"status":429,"error":"Too Many Requests",\
                    "message":"Create rate limit exceeded, retry later","timestamp":"%s"}"""
                                    .formatted(Instant.now(clock)));
            return;
        }
        chain.doFilter(request, response);
    }

    private void pruneStaleWindows(long nowMinute) {
        for (Iterator<Window> it = windows.values().iterator(); it.hasNext(); ) {
            if (it.next().epochMinute() < nowMinute) {
                it.remove();
            }
        }
    }
}
