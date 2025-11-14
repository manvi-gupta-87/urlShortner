package com.urlshortener.controller;

import com.urlshortener.dto.UrlRequestDto;
import com.urlshortener.dto.UrlResponseDto;
import com.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping
    public ResponseEntity<UrlResponseDto> createShortUrl(@Valid @RequestBody UrlRequestDto request,
                                                         Principal principal) {
        return ResponseEntity.ok(urlService.createShortUrl(request, principal.getName()));
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

    // TODO: Analytics endpoint will be added in Phase 6 when analytics-service is created

    @GetMapping
    public ResponseEntity<List<UrlResponseDto>> getUserUrls(Principal principal) {
        return ResponseEntity.ok(urlService.getAllUserUrls(principal.getName()));
    }
} 