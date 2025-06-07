package com.urlshortener.service.impl;

import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.Url;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.dto.UrlAnalyticsResponse;
import com.urlshortener.exception.UrlNotFoundException;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Async("analyticsTaskExecutor")
    @Transactional
    public CompletableFuture<Void> trackClick(Long urlId, String ipAddress, String userAgent, String referrer) {
        return CompletableFuture.runAsync(() -> {
            try {
                Url url = urlRepository.findById(urlId)
                        .orElseThrow(() -> new RuntimeException("URL not found"));

                UserAgent agent = UserAgent.parseUserAgentString(userAgent);
                
                ClickEvent clickEvent = ClickEvent.builder()
                        .url(url)
                        .timestamp(java.time.LocalDateTime.now())
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .referrer(referrer)
                        .browser(agent.getBrowser().getName())
                        .deviceType(agent.getOperatingSystem().getDeviceType().getName())
                        .build();

                clickEventRepository.save(clickEvent);
            } catch (Exception e) {
                // Log the error but don't throw it to prevent affecting the main flow
                e.printStackTrace();
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UrlAnalyticsResponse getUrlAnalytics(String shortCode, int days) {
        Url url = urlRepository.findByShortUrl(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found with short code: " + shortCode));

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        return UrlAnalyticsResponse.builder()
                .shortCode(shortCode)
                .originalUrl(url.getOriginalUrl())
                .totalClicks(getTotalClicks(url.getId()))
                .clicksByDate(getClicksByDate(url.getId(), startDate))
                .clicksByBrowser(getClicksByBrowser(url.getId()))
                .clicksByDeviceType(getClicksByDeviceType(url.getId()))
                .clicksByCountry(getClicksByCountry(url.getId()))
                .build();
    }

    private Map<String, Long> getClicksByDate(Long urlId, LocalDateTime startDate) {
        return clickEventRepository.getClicksByDate(urlId, startDate).stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.toMap(
                    row -> {
                        Object dateObj = row[0];
                        if (dateObj instanceof java.sql.Date) {
                            return ((java.sql.Date) dateObj).toLocalDate().format(DATE_FORMATTER);
                        } else if (dateObj instanceof java.util.Date) {
                            return ((java.util.Date) dateObj).toInstant()
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                                    .format(DATE_FORMATTER);
                        } else {
                            return dateObj.toString();
                        }
                    },
                    row -> (Long) row[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClickEvent> getClickEvents(Long urlId) {
        return clickEventRepository.findByUrlIdOrderByTimestampDesc(urlId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalClicks(Long urlId) {
        return clickEventRepository.countByUrlId(urlId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getClicksByCountry(Long urlId) {
        return clickEventRepository.getClicksByCountry(urlId).stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getClicksByBrowser(Long urlId) {
        return clickEventRepository.getClicksByBrowser(urlId).stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getClicksByDeviceType(Long urlId) {
        return clickEventRepository.getClicksByDeviceType(urlId).stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
    }
} 