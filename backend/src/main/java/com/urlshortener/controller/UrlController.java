package com.urlshortener.controller;

import com.urlshortener.dto.UrlRequestDto;
import com.urlshortener.dto.UrlResponseDto;
import com.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping
    public ResponseEntity<UrlResponseDto> createShortUrl(@Valid @RequestBody UrlRequestDto request) {
        return ResponseEntity.ok(urlService.createShortUrl(request));
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<UrlResponseDto> getOriginalUrl(@PathVariable String shortUrl) {
        return ResponseEntity.ok(urlService.getOriginalUrl(shortUrl));
    }
} 