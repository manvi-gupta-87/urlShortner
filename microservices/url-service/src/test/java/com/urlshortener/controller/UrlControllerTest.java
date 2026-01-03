package com.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.UrlAnalyticsResponse;
import com.urlshortener.dto.UrlRequestDto;
import com.urlshortener.dto.UrlResponseDto;
import com.urlshortener.exception.UrlDeactivatedException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.AnalyticsClientService;
import com.urlshortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UrlController.
 * Tests HTTP endpoints with MockMvc.
 */
@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlService urlService;

    @MockBean
    private AnalyticsClientService analyticsClientService;

    @MockBean
    private UrlRepository urlRepository;

    private UrlResponseDto testResponse;
    private UrlRequestDto testRequest;

    @BeforeEach
    void setUp() {
        testResponse = UrlResponseDto.builder()
                .id(1L)
                .originalUrl("https://example.com/long/url")
                .shortUrl("abc123XY")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .clickCount(0)
                .deactivated(false)
                .build();

        testRequest = new UrlRequestDto();
        testRequest.setUrl("https://example.com/long/url");
        testRequest.setExpirationDays(7);
    }

    @Nested
    @DisplayName("POST /api/v1/urls - createShortUrl")
    class CreateShortUrlTests {

        @Test
        @WithMockUser
        @DisplayName("Should create short URL successfully")
        void createShortUrl_Success() throws Exception {
            when(urlService.createShortUrl(any(UrlRequestDto.class), eq("testuser")))
                    .thenReturn(testResponse);

            mockMvc.perform(post("/api/v1/urls")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Name", "testuser")
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shortUrl").value("abc123XY"))
                    .andExpect(jsonPath("$.originalUrl").value("https://example.com/long/url"))
                    .andExpect(jsonPath("$.clickCount").value(0))
                    .andExpect(jsonPath("$.deactivated").value(false));
        }

    }

    @Nested
    @DisplayName("GET /api/v1/urls/{shortUrl} - getOriginalUrl")
    class GetOriginalUrlTests {

        @Test
        @WithMockUser
        @DisplayName("Should return URL successfully")
        void getOriginalUrl_Success() throws Exception {
            when(urlService.getOriginalUrl("abc123XY")).thenReturn(testResponse);

            mockMvc.perform(get("/api/v1/urls/abc123XY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.originalUrl").value("https://example.com/long/url"))
                    .andExpect(jsonPath("$.shortUrl").value("abc123XY"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when URL not found")
        void getOriginalUrl_NotFound() throws Exception {
            when(urlService.getOriginalUrl("nonexistent"))
                    .thenThrow(new UrlNotFoundException("URL not found"));

            mockMvc.perform(get("/api/v1/urls/nonexistent"))
                    .andExpect(status().isNotFound());
        }

    }

    @Nested
    @DisplayName("DELETE /api/v1/urls/{shortUrl} - deactivateUrl")
    class DeactivateUrlTests {

        @Test
        @WithMockUser
        @DisplayName("Should deactivate URL successfully")
        void deactivateUrl_Success() throws Exception {
            mockMvc.perform(delete("/api/v1/urls/abc123XY")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when URL not found")
        void deactivateUrl_NotFound() throws Exception {
            org.mockito.Mockito.doThrow(new UrlNotFoundException("URL not found"))
                    .when(urlService).deactivateUrl("nonexistent");

            mockMvc.perform(delete("/api/v1/urls/nonexistent")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/urls - getUserUrls")
    class GetUserUrlsTests {

        @Test
        @WithMockUser
        @DisplayName("Should return user URLs")
        void getUserUrls_Success() throws Exception {
            when(urlService.getAllUserUrls("testuser"))
                    .thenReturn(Arrays.asList(testResponse));

            mockMvc.perform(get("/api/v1/urls")
                            .header("X-User-Name", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].shortUrl").value("abc123XY"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return empty list when no URLs")
        void getUserUrls_EmptyList() throws Exception {
            when(urlService.getAllUserUrls("testuser"))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/urls")
                            .header("X-User-Name", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/urls/{shortCode}/stats - getUrlStats")
    class GetUrlStatsTests {

        @Test
        @WithMockUser
        @DisplayName("Should return URL stats with circuit breaker")
        void getUrlStats_Success() throws Exception {
            Url testUrl = Url.builder()
                    .id(1L)
                    .shortUrl("abc123XY")
                    .originalUrl("https://example.com")
                    .build();

            UrlAnalyticsResponse analyticsResponse = UrlAnalyticsResponse.builder()
                    .urlId(1L)
                    .shortCode("abc123XY")
                    .originalUrl("https://example.com")
                    .totalClicks(100L)
                    .build();

            when(urlRepository.findByShortUrl("abc123XY")).thenReturn(Optional.of(testUrl));
            when(analyticsClientService.getUrlAnalytics(1L, "abc123XY", "https://example.com", 7))
                    .thenReturn(analyticsResponse);

            mockMvc.perform(get("/api/v1/urls/abc123XY/stats")
                            .param("days", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalClicks").value(100))
                    .andExpect(jsonPath("$.shortCode").value("abc123XY"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return fallback when analytics unavailable")
        void getUrlStats_Fallback() throws Exception {
            Url testUrl = Url.builder()
                    .id(1L)
                    .shortUrl("abc123XY")
                    .originalUrl("https://example.com")
                    .build();

            UrlAnalyticsResponse fallbackResponse = UrlAnalyticsResponse.builder()
                    .urlId(1L)
                    .shortCode("abc123XY")
                    .originalUrl("https://example.com")
                    .totalClicks(0L)
                    .message("Analytics temporarily unavailable. URL data is current.")
                    .build();

            when(urlRepository.findByShortUrl("abc123XY")).thenReturn(Optional.of(testUrl));
            when(analyticsClientService.getUrlAnalytics(anyLong(), anyString(), anyString(), anyInt()))
                    .thenReturn(fallbackResponse);

            mockMvc.perform(get("/api/v1/urls/abc123XY/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Analytics temporarily unavailable. URL data is current."));
        }
    }
}
