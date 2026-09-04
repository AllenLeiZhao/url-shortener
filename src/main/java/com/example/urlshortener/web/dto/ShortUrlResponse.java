package com.example.urlshortener.web.dto;

import com.example.urlshortener.model.ShortUrl;
import java.time.Instant;

public record ShortUrlResponse(String code, String shortUrl, String originalUrl, Instant createdAt) {

    public static ShortUrlResponse of(ShortUrl entity, String shortUrl) {
        return new ShortUrlResponse(entity.getCode(), shortUrl, entity.getOriginalUrl(), entity.getCreatedAt());
    }
}
