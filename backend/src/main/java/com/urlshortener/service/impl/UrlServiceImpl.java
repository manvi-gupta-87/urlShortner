package com.urlshortener.service.impl;

import com.urlshortener.dto.UrlRequestDto;
import com.urlshortener.dto.UrlResponseDto;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.UrlService;
import com.urlshortener.service.generator.UrlGeneratorFactory;
import com.urlshortener.service.generator.UrlGeneratorFactory.GeneratorStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final UrlGeneratorFactory urlGeneratorFactory;

    @Value("${url.generator.strategy:DISTRIBUTED}")
    private GeneratorStrategy generatorStrategy;

    @Override
    @Transactional
    public UrlResponseDto createShortUrl(UrlRequestDto request) {
        String shortUrl;
        do {
            shortUrl = urlGeneratorFactory.getGenerator(generatorStrategy)
                    .generateShortUrl();
        } while (urlRepository.existsByShortUrl(shortUrl));

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(
            request.getExpirationDays() != null ? request.getExpirationDays() : 7
        );

        Url url = Url.builder()
                .originalUrl(request.getUrl())
                .shortUrl(shortUrl)
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .clickCount(0)
                .build();

        url = urlRepository.save(url);

        return UrlResponseDto.builder()
                .originalUrl(url.getOriginalUrl())
                .shortUrl(url.getShortUrl())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .build();
    }

    @Override
    @Transactional
    public UrlResponseDto getOriginalUrl(String shortUrl) {
        Url url = urlRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        if (url.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("URL has expired");
        }

        url.setClickCount(url.getClickCount() + 1);
        url = urlRepository.save(url);

        return UrlResponseDto.builder()
                .originalUrl(url.getOriginalUrl())
                .shortUrl(url.getShortUrl())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .build();
    }
} 