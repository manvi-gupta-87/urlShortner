package com.urlshortener.service;

import com.urlshortener.dto.UrlRequestDto;
import com.urlshortener.dto.UrlResponseDto;
import com.urlshortener.dto.UserDto;
import com.urlshortener.exception.UrlDeactivatedException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.lib.AuthServiceClient;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.generator.UrlGeneratorStrategy;
import com.urlshortener.service.generator.UrlGeneratorFactory;
import com.urlshortener.service.impl.UrlServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UrlServiceImpl.
 * Tests all business logic scenarios including edge cases.
 */
@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UrlGeneratorFactory urlGeneratorFactory;

    @Mock
    private UrlGeneratorStrategy urlGeneratorStrategy;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private UrlServiceImpl urlService;

    private UserDto testUser;
    private Url testUrl;
    private UrlRequestDto testRequest;

    @BeforeEach
    void setUp() {
        // Set the generator strategy via reflection
        ReflectionTestUtils.setField(urlService, "generatorStrategy",
            UrlGeneratorFactory.GeneratorStrategy.DISTRIBUTED);

        testUser = UserDto.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        testUrl = Url.builder()
                .id(1L)
                .originalUrl("https://example.com/long/url/path")
                .shortUrl("abc123XY")
                .userId(1L)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .clickCount(0)
                .deactivated(false)
                .build();

        testRequest = new UrlRequestDto();
        testRequest.setUrl("https://example.com/long/url/path");
        testRequest.setExpirationDays(7);
    }

    @Nested
    @DisplayName("createShortUrl tests")
    class CreateShortUrlTests {

        @Test
        @DisplayName("Should create short URL successfully")
        void createShortUrl_Success() {
            // Given
            when(authServiceClient.getUserByUsername("testuser")).thenReturn(testUser);
            when(urlGeneratorFactory.getGenerator(any())).thenReturn(urlGeneratorStrategy);
            when(urlGeneratorStrategy.generateShortUrl()).thenReturn("abc123XY");
            when(urlRepository.existsByShortUrl("abc123XY")).thenReturn(false);
            when(urlRepository.save(any(Url.class))).thenReturn(testUrl);

            // When
            UrlResponseDto result = urlService.createShortUrl(testRequest, "testuser");

            // Then
            assertNotNull(result);
            assertEquals("abc123XY", result.getShortUrl());
            assertEquals("https://example.com/long/url/path", result.getOriginalUrl());
            assertEquals(0, result.getClickCount());
            assertFalse(result.isDeactivated());

            verify(urlRepository).save(any(Url.class));
            verify(authServiceClient).getUserByUsername("testuser");
        }

        @Test
        @DisplayName("Should handle collision and regenerate short URL")
        void createShortUrl_HandlesCollision() {
            // Given
            when(authServiceClient.getUserByUsername("testuser")).thenReturn(testUser);
            when(urlGeneratorFactory.getGenerator(any())).thenReturn(urlGeneratorStrategy);
            when(urlGeneratorStrategy.generateShortUrl())
                    .thenReturn("abc123XY")  // First attempt - collides
                    .thenReturn("def456YZ"); // Second attempt - succeeds
            when(urlRepository.existsByShortUrl("abc123XY")).thenReturn(true);
            when(urlRepository.existsByShortUrl("def456YZ")).thenReturn(false);
            when(urlRepository.save(any(Url.class))).thenReturn(testUrl);

            // When
            urlService.createShortUrl(testRequest, "testuser");

            // Then
            verify(urlGeneratorStrategy, times(2)).generateShortUrl();
            verify(urlRepository).existsByShortUrl("abc123XY");
            verify(urlRepository).existsByShortUrl("def456YZ");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void createShortUrl_ThrowsException_WhenUserNotFound() {
            // Given
            when(authServiceClient.getUserByUsername("nonexistent")).thenReturn(null);
            when(urlGeneratorFactory.getGenerator(any())).thenReturn(urlGeneratorStrategy);
            when(urlGeneratorStrategy.generateShortUrl()).thenReturn("abc123XY");
            when(urlRepository.existsByShortUrl(anyString())).thenReturn(false);

            // When / Then
            assertThrows(UsernameNotFoundException.class, () ->
                    urlService.createShortUrl(testRequest, "nonexistent")
            );

            verify(urlRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should use default expiration when not specified")
        void createShortUrl_UsesDefaultExpiration() {
            // Given
            testRequest.setExpirationDays(null);
            when(authServiceClient.getUserByUsername("testuser")).thenReturn(testUser);
            when(urlGeneratorFactory.getGenerator(any())).thenReturn(urlGeneratorStrategy);
            when(urlGeneratorStrategy.generateShortUrl()).thenReturn("abc123XY");
            when(urlRepository.existsByShortUrl("abc123XY")).thenReturn(false);
            when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> {
                Url savedUrl = invocation.getArgument(0);
                // Verify expiration is approximately 7 days from now
                assertTrue(savedUrl.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
                assertTrue(savedUrl.getExpiresAt().isBefore(LocalDateTime.now().plusDays(8)));
                return testUrl;
            });

            // When
            urlService.createShortUrl(testRequest, "testuser");

            // Then
            verify(urlRepository).save(any(Url.class));
        }
    }

    @Nested
    @DisplayName("getOriginalUrl tests")
    class GetOriginalUrlTests {

        @Test
        @DisplayName("Should return URL and increment click count")
        void getOriginalUrl_Success_IncrementsClickCount() {
            // Given
            when(urlRepository.findByShortUrl("abc123XY")).thenReturn(Optional.of(testUrl));
            when(urlRepository.save(any(Url.class))).thenReturn(testUrl);

            // When
            UrlResponseDto result = urlService.getOriginalUrl("abc123XY");

            // Then
            assertNotNull(result);
            assertEquals("https://example.com/long/url/path", result.getOriginalUrl());
            assertEquals(1, testUrl.getClickCount()); // Verify increment
            verify(urlRepository).save(testUrl);
        }

        @Test
        @DisplayName("Should throw UrlNotFoundException when URL not found")
        void getOriginalUrl_ThrowsException_WhenNotFound() {
            // Given
            when(urlRepository.findByShortUrl("nonexistent")).thenReturn(Optional.empty());

            // When / Then
            UrlNotFoundException exception = assertThrows(UrlNotFoundException.class, () ->
                    urlService.getOriginalUrl("nonexistent")
            );

            assertTrue(exception.getMessage().contains("nonexistent"));
        }

        @Test
        @DisplayName("Should throw UrlDeactivatedException when URL is deactivated")
        void getOriginalUrl_ThrowsException_WhenDeactivated() {
            // Given
            testUrl.setDeactivated(true);
            when(urlRepository.findByShortUrl("abc123XY")).thenReturn(Optional.of(testUrl));

            // When / Then
            UrlDeactivatedException exception = assertThrows(UrlDeactivatedException.class, () ->
                    urlService.getOriginalUrl("abc123XY")
            );

            assertTrue(exception.getMessage().contains("deactivated"));
            verify(urlRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw UrlExpiredException when URL is expired")
        void getOriginalUrl_ThrowsException_WhenExpired() {
            // Given
            testUrl.setExpiresAt(LocalDateTime.now().minusDays(1));
            when(urlRepository.findByShortUrl("abc123XY")).thenReturn(Optional.of(testUrl));

            // When / Then
            UrlExpiredException exception = assertThrows(UrlExpiredException.class, () ->
                    urlService.getOriginalUrl("abc123XY")
            );

            assertTrue(exception.getMessage().contains("expired"));
            verify(urlRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllUserUrls tests")
    class GetAllUserUrlsTests {

        @Test
        @DisplayName("Should return all user URLs ordered by creation date")
        void getAllUserUrls_ReturnsUrls() {
            // Given
            Url url1 = createUrl(1L, "abc1", LocalDateTime.now().minusDays(2));
            Url url2 = createUrl(2L, "abc2", LocalDateTime.now().minusDays(1));
            Url url3 = createUrl(3L, "abc3", LocalDateTime.now());

            when(authServiceClient.getUserByUsername("testuser")).thenReturn(testUser);
            when(urlRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Arrays.asList(url3, url2, url1));

            // When
            List<UrlResponseDto> result = urlService.getAllUserUrls("testuser");

            // Then
            assertEquals(3, result.size());
            assertEquals("abc3", result.get(0).getShortUrl()); // Most recent first
        }

        @Test
        @DisplayName("Should return empty list when username is empty")
        void getAllUserUrls_ReturnsEmptyList_WhenUsernameEmpty() {
            // When
            List<UrlResponseDto> result = urlService.getAllUserUrls("");

            // Then
            assertTrue(result.isEmpty());
            verify(authServiceClient, never()).getUserByUsername(any());
        }

        @Test
        @DisplayName("Should return empty list when username is null")
        void getAllUserUrls_ReturnsEmptyList_WhenUsernameNull() {
            // When
            List<UrlResponseDto> result = urlService.getAllUserUrls(null);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list when user not found")
        void getAllUserUrls_ReturnsEmptyList_WhenUserNotFound() {
            // Given
            when(authServiceClient.getUserByUsername("unknown")).thenReturn(null);

            // When
            List<UrlResponseDto> result = urlService.getAllUserUrls("unknown");

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list when user has no URLs")
        void getAllUserUrls_ReturnsEmptyList_WhenNoUrls() {
            // Given
            when(authServiceClient.getUserByUsername("testuser")).thenReturn(testUser);
            when(urlRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Collections.emptyList());

            // When
            List<UrlResponseDto> result = urlService.getAllUserUrls("testuser");

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("deactivateUrl tests")
    class DeactivateUrlTests {

        @Test
        @DisplayName("Should deactivate URL successfully")
        void deactivateUrl_Success() {
            // Given
            when(urlRepository.findByShortUrl("abc123XY")).thenReturn(Optional.of(testUrl));
            when(urlRepository.save(any(Url.class))).thenReturn(testUrl);

            // When
            urlService.deactivateUrl("abc123XY");

            // Then
            assertTrue(testUrl.getDeactivated());
            verify(urlRepository).save(testUrl);
        }

        @Test
        @DisplayName("Should throw UrlNotFoundException when URL not found")
        void deactivateUrl_ThrowsException_WhenNotFound() {
            // Given
            when(urlRepository.findByShortUrl("nonexistent")).thenReturn(Optional.empty());

            // When / Then
            assertThrows(UrlNotFoundException.class, () ->
                    urlService.deactivateUrl("nonexistent")
            );

            verify(urlRepository, never()).save(any());
        }
    }

    // Helper method to create test URLs
    private Url createUrl(Long id, String shortUrl, LocalDateTime createdAt) {
        return Url.builder()
                .id(id)
                .shortUrl(shortUrl)
                .originalUrl("https://example.com/" + shortUrl)
                .userId(1L)
                .createdAt(createdAt)
                .expiresAt(createdAt.plusDays(7))
                .clickCount(0)
                .deactivated(false)
                .build();
    }
}
