package com.urlshortener.service;

import com.urlshortener.dto.UrlRequestDto;
import com.urlshortener.dto.UrlResponseDto;

import java.util.List;

public interface UrlService {
    UrlResponseDto createShortUrl(UrlRequestDto request, String userName);
    UrlResponseDto getOriginalUrl(String shortUrl);
    void incrementClickCount(String shortUrl);
    void deactivateUrl(String shortUrl);
    List<UrlResponseDto> getAllUserUrls(String userName);
} 