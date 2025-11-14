package com.urlshortener.service.generator;

/**
 * Strategy interface for URL generation algorithms.
 * Implementations should provide thread-safe URL generation.
 */
public interface UrlGeneratorStrategy {
    /**
     * Generates a unique short URL.
     * @return A unique string to be used as the short URL
     */
    String generateShortUrl();
} 