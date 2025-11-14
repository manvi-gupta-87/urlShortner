package com.urlshortener.controller;

import com.urlshortener.dto.UrlAnalyticsResponse;
import com.urlshortener.model.ClickEvent;
import com.urlshortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/urls/{urlId}/clicks")
    public ResponseEntity<List<ClickEvent>> getClickEvents(@PathVariable Long urlId) {
        return ResponseEntity.ok(analyticsService.getClickEvents(urlId));
    }

    @GetMapping("/urls/{urlId}/total-clicks")
    public ResponseEntity<Long> getTotalClicks(@PathVariable Long urlId) {
        return ResponseEntity.ok(analyticsService.getTotalClicks(urlId));
    }

    @GetMapping("/urls/{urlId}/clicks-by-country")
    public ResponseEntity<Map<String, Long>> getClicksByCountry(@PathVariable Long urlId) {
        return ResponseEntity.ok(analyticsService.getClicksByCountry(urlId));
    }

    @GetMapping("/urls/{urlId}/clicks-by-browser")
    public ResponseEntity<Map<String, Long>> getClicksByBrowser(@PathVariable Long urlId) {
        return ResponseEntity.ok(analyticsService.getClicksByBrowser(urlId));
    }

    @GetMapping("/urls/{urlId}/clicks-by-device")
    public ResponseEntity<Map<String, Long>> getClicksByDeviceType(@PathVariable Long urlId) {
        return ResponseEntity.ok(analyticsService.getClicksByDeviceType(urlId));
    }

    @GetMapping("/urls/stats")
    public ResponseEntity<UrlAnalyticsResponse> getUrlAnalytics(
            @RequestParam Long urlId,
            @RequestParam String shortCode,
            @RequestParam String originalUrl,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsService.getUrlAnalytics(urlId, shortCode, originalUrl, days));
    }
} 