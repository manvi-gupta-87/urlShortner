package com.urlshortener.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class UrlAnalyticsResponse {
    private Long urlId;
    private String shortCode;
    private String originalUrl;
    private Long totalClicks;
    private Map<String, Long> clicksByDate;
    private Map<String, Long> clicksByBrowser;
    private Map<String, Long> clicksByDeviceType;
    private Map<String, Long> clicksByCountry;

    // Message field for fallback responses when analytics is unavailable
    private String message;
} 