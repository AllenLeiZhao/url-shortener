package com.example.urlshortener.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl, ShortCode shortCode, RateLimit rateLimit) {

    public record ShortCode(int length, int maxCollisionRetries) {}

    public record RateLimit(int createPerMinute) {}
}
