package com.urlshortener.service;

import com.urlshortener.model.ClickEvent;
import com.urlshortener.dto.UrlAnalyticsResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AnalyticsService {
    CompletableFuture<Void> trackClick(Long urlId, String ipAddress, String userAgent, String referrer);
    List<ClickEvent> getClickEvents(Long urlId);
    Long getTotalClicks(Long urlId);
    Map<String, Long> getClicksByCountry(Long urlId);
    Map<String, Long> getClicksByBrowser(Long urlId);
    Map<String, Long> getClicksByDeviceType(Long urlId);
    UrlAnalyticsResponse getUrlAnalytics(Long urlId, String shortCode, String originalUrl, int days);
} 