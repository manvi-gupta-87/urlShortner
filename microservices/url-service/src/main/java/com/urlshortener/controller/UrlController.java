package com.urlshortener.controller;

import com.urlshortener.dto.UrlAnalyticsResponse;
import com.urlshortener.dto.UrlRequestDto;
import com.urlshortener.dto.UrlResponseDto;
import com.urlshortener.lib.AnalyticsServiceClient;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final AnalyticsServiceClient analyticsServiceClient;
    private final UrlRepository urlRepository;

    @PostMapping
    public ResponseEntity<UrlResponseDto> createShortUrl(
            @Valid @RequestBody UrlRequestDto request,
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(urlService.createShortUrl(request, username));
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<UrlResponseDto> getOriginalUrl(@PathVariable String shortUrl) {
        return ResponseEntity.ok(urlService.getOriginalUrl(shortUrl));
    }

    @DeleteMapping("/{shortUrl}")
    public ResponseEntity<Void> deactivateUrl(@PathVariable String shortUrl) {
        urlService.deactivateUrl(shortUrl);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<UrlAnalyticsResponse> getUrlStats(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "7") int days) {
        // Fetch URL data from url-service database
        Url url = urlRepository.findByShortUrl(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        // Call analytics-service via Feign client
        return ResponseEntity.ok(
            analyticsServiceClient.getUrlAnalytics(
                url.getId(),
                shortCode,
                url.getOriginalUrl(),
                days
            )
        );
    }

    @GetMapping
    public ResponseEntity<List<UrlResponseDto>> getUserUrls(
            @RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(urlService.getAllUserUrls(username));
    }
} 