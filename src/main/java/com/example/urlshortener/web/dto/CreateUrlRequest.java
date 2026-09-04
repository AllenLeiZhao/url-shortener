package com.example.urlshortener.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUrlRequest(@NotBlank(message = "url is required") String url) {}
