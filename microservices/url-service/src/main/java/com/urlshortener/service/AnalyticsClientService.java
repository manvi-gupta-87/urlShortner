package com.urlshortener.service;

import com.urlshortener.dto.UrlAnalyticsResponse;
import com.urlshortener.lib.AnalyticsServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service wrapper for Analytics Service client calls.
 *
 * WHY THIS EXISTS:
 * - Wraps Feign client calls with Circuit Breaker pattern
 * - Provides fallback when Analytics Service is unavailable
 * - Prevents cascade failures from bringing down URL Service
 *
 * HOW IT WORKS:
 * - @CircuitBreaker monitors success/failure of calls
 * - If failure rate exceeds threshold (50%), circuit OPENS
 * - OPEN state: calls fallback immediately (no network call)
 * - After wait time, circuit goes HALF-OPEN to test
 * - If test succeeds, circuit CLOSES and resumes normal operation
 *
 * INTERVIEW TIP:
 * "The Circuit Breaker prevents cascade failures. If Analytics Service
 * is down, instead of URL Service waiting for timeouts and exhausting
 * threads, we fail fast and return a graceful fallback response."
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsClientService {

    private final AnalyticsServiceClient analyticsServiceClient;

    /**
     * Get URL analytics with circuit breaker protection.
     *
     * @CircuitBreaker parameters:
     * - name: identifies this circuit breaker instance (for config and metrics)
     * - fallbackMethod: method to call when circuit is OPEN or call fails
     */
    @CircuitBreaker(name = "analyticsService", fallbackMethod = "getUrlAnalyticsFallback")
    public UrlAnalyticsResponse getUrlAnalytics(Long urlId, String shortCode,
                                                 String originalUrl, int days) {
        log.debug("Calling Analytics Service for URL: {}", shortCode);
        return analyticsServiceClient.getUrlAnalytics(urlId, shortCode, originalUrl, days);
    }

    /**
     * Fallback method when Analytics Service is unavailable.
     *
     * IMPORTANT:
     * - Method signature must match original + Exception parameter
     * - Returns graceful degradation response (not error)
     * - Logs the failure for monitoring
     *
     * WHAT HAPPENS:
     * - User still gets URL info (from URL Service DB)
     * - Analytics data shows "unavailable" instead of error
     * - System remains functional even if Analytics is down
     */
    public UrlAnalyticsResponse getUrlAnalyticsFallback(Long urlId, String shortCode,
                                                         String originalUrl, int days,
                                                         Exception ex) {
        log.warn("Analytics Service unavailable. Circuit breaker fallback triggered. " +
                 "URL: {}, Error: {}", shortCode, ex.getMessage());

        // Return a graceful degradation response
        return UrlAnalyticsResponse.builder()
                .urlId(urlId)
                .shortCode(shortCode)
                .originalUrl(originalUrl)
                .totalClicks(0L)
                .message("Analytics temporarily unavailable. URL data is current.")
                .build();
    }
}
