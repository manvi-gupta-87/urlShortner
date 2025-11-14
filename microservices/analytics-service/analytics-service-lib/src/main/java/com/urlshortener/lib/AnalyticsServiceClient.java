package com.urlshortener.lib;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "analytics-service")
public interface AnalyticsServiceClient {

    @GetMapping("/api/v1/analytics/urls/{urlId}/total-clicks")
    Long getTotalClicks(@PathVariable Long urlId);

    @GetMapping("/api/v1/analytics/urls/{urlId}/clicks-by-country")
    Map<String, Long> getClicksByCountry(@PathVariable Long urlId);

    @GetMapping("/api/v1/analytics/urls/{urlId}/clicks-by-browser")
    Map<String, Long> getClicksByBrowser(@PathVariable Long urlId);

    @GetMapping("/api/v1/analytics/urls/{urlId}/clicks-by-device")
    Map<String, Long> getClicksByDeviceType(@PathVariable Long urlId);
}
