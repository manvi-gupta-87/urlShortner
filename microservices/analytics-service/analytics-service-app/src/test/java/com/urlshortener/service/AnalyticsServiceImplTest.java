package com.urlshortener.service;

import com.urlshortener.dto.UrlAnalyticsResponse;
import com.urlshortener.model.ClickEvent;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsServiceImpl.
 * Tests click tracking and analytics aggregation logic.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private ClickEventRepository clickEventRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private ClickEvent testClickEvent;

    @BeforeEach
    void setUp() {
        testClickEvent = ClickEvent.builder()
                .id(1L)
                .urlId(1L)
                .timestamp(LocalDateTime.now())
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/91.0.4472.124")
                .referrer("https://google.com")
                .browser("Chrome")
                .deviceType("Computer")
                .country("US")
                .build();
    }

    @Nested
    @DisplayName("getUrlAnalytics tests")
    class GetUrlAnalyticsTests {

        @Test
        @DisplayName("Should return complete analytics response")
        void getUrlAnalytics_Success() {
            // Given
            Long urlId = 1L;
            when(clickEventRepository.countByUrlId(urlId)).thenReturn(100L);
            when(clickEventRepository.getClicksByDate(eq(urlId), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(
                            new Object[]{"2024-01-01", 50L},
                            new Object[]{"2024-01-02", 50L}
                    ));
            when(clickEventRepository.getClicksByBrowser(urlId))
                    .thenReturn(Arrays.asList(
                            new Object[]{"Chrome", 60L},
                            new Object[]{"Firefox", 40L}
                    ));
            when(clickEventRepository.getClicksByDeviceType(urlId))
                    .thenReturn(Arrays.asList(
                            new Object[]{"Computer", 70L},
                            new Object[]{"Mobile", 30L}
                    ));
            when(clickEventRepository.getClicksByCountry(urlId))
                    .thenReturn(Arrays.asList(
                            new Object[]{"US", 80L},
                            new Object[]{"UK", 20L}
                    ));

            // When
            UrlAnalyticsResponse result = analyticsService.getUrlAnalytics(
                    urlId, "abc123", "https://example.com", 7);

            // Then
            assertNotNull(result);
            assertEquals("abc123", result.getShortCode());
            assertEquals("https://example.com", result.getOriginalUrl());
            assertEquals(100L, result.getTotalClicks());
            assertEquals(2, result.getClicksByDate().size());
            assertEquals(2, result.getClicksByBrowser().size());
            assertEquals(60L, result.getClicksByBrowser().get("Chrome"));
        }

        @Test
        @DisplayName("Should return zero clicks when no data")
        void getUrlAnalytics_ZeroClicks() {
            // Given
            Long urlId = 1L;
            when(clickEventRepository.countByUrlId(urlId)).thenReturn(0L);
            when(clickEventRepository.getClicksByDate(eq(urlId), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());
            when(clickEventRepository.getClicksByBrowser(urlId))
                    .thenReturn(Collections.emptyList());
            when(clickEventRepository.getClicksByDeviceType(urlId))
                    .thenReturn(Collections.emptyList());
            when(clickEventRepository.getClicksByCountry(urlId))
                    .thenReturn(Collections.emptyList());

            // When
            UrlAnalyticsResponse result = analyticsService.getUrlAnalytics(
                    urlId, "abc123", "https://example.com", 7);

            // Then
            assertEquals(0L, result.getTotalClicks());
            assertTrue(result.getClicksByDate().isEmpty());
            assertTrue(result.getClicksByBrowser().isEmpty());
        }

        @Test
        @DisplayName("Should handle null values in aggregation results")
        void getUrlAnalytics_HandlesNullValues() {
            // Given
            Long urlId = 1L;
            when(clickEventRepository.countByUrlId(urlId)).thenReturn(10L);
            when(clickEventRepository.getClicksByDate(eq(urlId), any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList(
                            new Object[]{null, 5L},  // Null date should be filtered
                            new Object[]{"2024-01-01", 5L}
                    ));
            when(clickEventRepository.getClicksByBrowser(urlId))
                    .thenReturn(Arrays.<Object[]>asList(new Object[]{null, 10L}));
            when(clickEventRepository.getClicksByDeviceType(urlId))
                    .thenReturn(Collections.emptyList());
            when(clickEventRepository.getClicksByCountry(urlId))
                    .thenReturn(Collections.emptyList());

            // When
            UrlAnalyticsResponse result = analyticsService.getUrlAnalytics(
                    urlId, "abc123", "https://example.com", 7);

            // Then
            assertEquals(1, result.getClicksByDate().size()); // Null filtered out
            assertTrue(result.getClicksByBrowser().isEmpty()); // Null filtered out
        }
    }

    @Nested
    @DisplayName("getTotalClicks tests")
    class GetTotalClicksTests {

        @Test
        @DisplayName("Should return total clicks count")
        void getTotalClicks_ReturnsCount() {
            // Given
            when(clickEventRepository.countByUrlId(1L)).thenReturn(150L);

            // When
            Long result = analyticsService.getTotalClicks(1L);

            // Then
            assertEquals(150L, result);
        }

        @Test
        @DisplayName("Should return zero for new URL")
        void getTotalClicks_ReturnsZeroForNewUrl() {
            // Given
            when(clickEventRepository.countByUrlId(999L)).thenReturn(0L);

            // When
            Long result = analyticsService.getTotalClicks(999L);

            // Then
            assertEquals(0L, result);
        }
    }

    @Nested
    @DisplayName("getClickEvents tests")
    class GetClickEventsTests {

        @Test
        @DisplayName("Should return click events ordered by timestamp desc")
        void getClickEvents_ReturnsOrderedEvents() {
            // Given
            ClickEvent event1 = ClickEvent.builder()
                    .id(1L)
                    .urlId(1L)
                    .timestamp(LocalDateTime.now().minusHours(2))
                    .build();
            ClickEvent event2 = ClickEvent.builder()
                    .id(2L)
                    .urlId(1L)
                    .timestamp(LocalDateTime.now().minusHours(1))
                    .build();
            ClickEvent event3 = ClickEvent.builder()
                    .id(3L)
                    .urlId(1L)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(clickEventRepository.findByUrlIdOrderByTimestampDesc(1L))
                    .thenReturn(Arrays.asList(event3, event2, event1));

            // When
            List<ClickEvent> result = analyticsService.getClickEvents(1L);

            // Then
            assertEquals(3, result.size());
            assertEquals(3L, result.get(0).getId()); // Most recent first
        }

        @Test
        @DisplayName("Should return empty list for URL with no clicks")
        void getClickEvents_ReturnsEmptyList() {
            // Given
            when(clickEventRepository.findByUrlIdOrderByTimestampDesc(999L))
                    .thenReturn(Collections.emptyList());

            // When
            List<ClickEvent> result = analyticsService.getClickEvents(999L);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getClicksByCountry tests")
    class GetClicksByCountryTests {

        @Test
        @DisplayName("Should return clicks grouped by country")
        void getClicksByCountry_ReturnsGroupedData() {
            // Given
            when(clickEventRepository.getClicksByCountry(1L))
                    .thenReturn(Arrays.asList(
                            new Object[]{"US", 100L},
                            new Object[]{"UK", 50L},
                            new Object[]{"DE", 25L}
                    ));

            // When
            var result = analyticsService.getClicksByCountry(1L);

            // Then
            assertEquals(3, result.size());
            assertEquals(100L, result.get("US"));
            assertEquals(50L, result.get("UK"));
        }
    }

    @Nested
    @DisplayName("getClicksByBrowser tests")
    class GetClicksByBrowserTests {

        @Test
        @DisplayName("Should return clicks grouped by browser")
        void getClicksByBrowser_ReturnsGroupedData() {
            // Given
            when(clickEventRepository.getClicksByBrowser(1L))
                    .thenReturn(Arrays.asList(
                            new Object[]{"Chrome", 200L},
                            new Object[]{"Firefox", 80L},
                            new Object[]{"Safari", 50L}
                    ));

            // When
            var result = analyticsService.getClicksByBrowser(1L);

            // Then
            assertEquals(3, result.size());
            assertEquals(200L, result.get("Chrome"));
        }
    }

    @Nested
    @DisplayName("getClicksByDeviceType tests")
    class GetClicksByDeviceTypeTests {

        @Test
        @DisplayName("Should return clicks grouped by device type")
        void getClicksByDeviceType_ReturnsGroupedData() {
            // Given
            when(clickEventRepository.getClicksByDeviceType(1L))
                    .thenReturn(Arrays.asList(
                            new Object[]{"Computer", 150L},
                            new Object[]{"Mobile", 100L},
                            new Object[]{"Tablet", 30L}
                    ));

            // When
            var result = analyticsService.getClicksByDeviceType(1L);

            // Then
            assertEquals(3, result.size());
            assertEquals(150L, result.get("Computer"));
            assertEquals(100L, result.get("Mobile"));
        }
    }
}
