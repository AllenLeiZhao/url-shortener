package com.example.urlshortener.web.dto;

import com.example.urlshortener.service.ClickTrackingService.UrlStats;
import java.time.Instant;

public record UrlStatsResponse(String code, long totalClicks, long clicksLast24h, Instant lastClickAt) {

    public static UrlStatsResponse of(String code, UrlStats stats) {
        return new UrlStatsResponse(code, stats.totalClicks(), stats.clicksLast24h(), stats.lastClickAt());
    }
}
