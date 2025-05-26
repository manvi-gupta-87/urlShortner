package com.urlshortener.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UrlResponseDto {
    private String originalUrl;
    private String shortUrl;
    private LocalDateTime expiresAt;
    private Integer clickCount;
} 