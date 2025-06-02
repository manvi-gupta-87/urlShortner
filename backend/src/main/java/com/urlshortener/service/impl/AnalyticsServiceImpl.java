package com.urlshortener.service.impl;

import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.Url;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.AnalyticsService;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    @Override
    @Transactional
    public void trackClick(Long urlId, String ipAddress, String userAgent, String referrer) {
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
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getClicksByBrowser(Long urlId) {
        return clickEventRepository.getClicksByBrowser(urlId).stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getClicksByDeviceType(Long urlId) {
        return clickEventRepository.getClicksByDeviceType(urlId).stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
    }
} 