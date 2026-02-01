package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlResponseDto {
    private Long id;
    private String originalUrl;
    private String shortUrl;
    private LocalDateTime expiresAt;
    private Integer clickCount;
    private boolean deactivated;
} 