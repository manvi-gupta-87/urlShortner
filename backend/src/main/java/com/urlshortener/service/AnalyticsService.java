package com.urlshortener.service;

import com.urlshortener.model.ClickEvent;
import java.util.List;
import java.util.Map;

public interface AnalyticsService {
    void trackClick(Long urlId, String ipAddress, String userAgent, String referrer);
    List<ClickEvent> getClickEvents(Long urlId);
    Long getTotalClicks(Long urlId);
    Map<String, Long> getClicksByCountry(Long urlId);
    Map<String, Long> getClicksByBrowser(Long urlId);
    Map<String, Long> getClicksByDeviceType(Long urlId);
} 