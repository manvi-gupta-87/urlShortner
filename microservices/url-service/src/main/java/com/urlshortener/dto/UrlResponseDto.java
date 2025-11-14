package com.urlshortener.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UrlResponseDto {
    private Long id;
    private String originalUrl;
    private String shortUrl;
    private LocalDateTime expiresAt;
    private Integer clickCount;
    private boolean deactivated;
} 