package com.urlshortener.service.impl;

import com.urlshortener.dto.UrlRequestDto;
import com.urlshortener.dto.UrlResponseDto;
import com.urlshortener.dto.UserDto;
import com.urlshortener.exception.UrlDeactivatedException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.lib.AuthServiceClient;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.UrlService;
import com.urlshortener.service.generator.UrlGeneratorFactory;
import com.urlshortener.service.generator.UrlGeneratorFactory.GeneratorStrategy;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final UrlGeneratorFactory urlGeneratorFactory;
    private final AuthServiceClient authServiceClient;

    @Value("${url.generator.strategy:DISTRIBUTED}")
    private GeneratorStrategy generatorStrategy;


    @Override
    @Transactional
    public UrlResponseDto createShortUrl(UrlRequestDto request, String userName) {
        String shortUrl;
        do {
            shortUrl = urlGeneratorFactory.getGenerator(generatorStrategy)
                    .generateShortUrl();
        } while (urlRepository.existsByShortUrl(shortUrl));

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(
            request.getExpirationDays() != null ? request.getExpirationDays() : 7
        );

        UserDto userDto = authServiceClient.getUserByUsername(userName);
        if (userDto == null) {
            throw new UsernameNotFoundException("User not found in the database " + userName);
        }
        Url url = Url.builder()
                .originalUrl(request.getUrl())
                .shortUrl(shortUrl)
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .clickCount(0)
                .deactivated(false)
                .userId(userDto.getId())
                .build();

        url = urlRepository.save(url);

        return UrlResponseDto.builder()
                .id(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortUrl(url.getShortUrl())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .deactivated(url.getDeactivated())
                .build();
    }

    @Override
    @Cacheable(value = "urls", key = "#shortUrl")
    @Transactional(readOnly = true)
    public UrlResponseDto getOriginalUrl(String shortUrl) {
        Url url = urlRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortUrl));

        if (url.getDeactivated()) {
            throw new UrlDeactivatedException("URL has been deactivated: " + shortUrl);
        }

        if (url.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException("URL has expired: " + shortUrl);
        }

        return UrlResponseDto.builder()
                .id(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortUrl(url.getShortUrl())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .deactivated(url.getDeactivated())
                .build();
    }

    @Override
    @Transactional
    public void incrementClickCount(String shortUrl) {
        urlRepository.incrementClickCount(shortUrl);
    }

    @Override
    @Transactional
    public List<UrlResponseDto> getAllUserUrls(String userName) {

        if (StringUtils.isEmpty(userName)) {
            log.error("UserName is empty");
            return Collections.EMPTY_LIST;
        }
        UserDto userDto = authServiceClient.getUserByUsername(userName);
        if (userDto != null) {
            List<Url> urls = urlRepository.findByUserIdOrderByCreatedAtDesc(userDto.getId());
            if (!CollectionUtils.isEmpty(urls)) {
                return urls.stream().map(url -> UrlResponseDto.builder()
                        .id(url.getId())
                        .originalUrl(url.getOriginalUrl())
                        .shortUrl(url.getShortUrl())
                        .expiresAt(url.getExpiresAt())
                        .clickCount(url.getClickCount())
                                .deactivated(url.getDeactivated())
                        .build())
                        .collect(Collectors.toList());

                };
            }
        return Collections.EMPTY_LIST;
    }

    @Override
    @Transactional
    @CacheEvict(value = "urls", key = "#shortUrl")
    public void deactivateUrl(String shortUrl) {
        Url url = urlRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortUrl));
        
        url.setDeactivated(true);
        urlRepository.save(url);
    }
} 