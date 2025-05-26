package com.urlshortener.service;

import com.urlshortener.dto.UrlRequestDto;
import com.urlshortener.dto.UrlResponseDto;

public interface UrlService {
    UrlResponseDto createShortUrl(UrlRequestDto request);
    UrlResponseDto getOriginalUrl(String shortUrl);
} 