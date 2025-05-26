package com.urlshortener.service.generator;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple URL generator that uses an atomic counter to generate unique URLs.
 * Thread Safety:
 * - Uses AtomicLong for thread-safe counter operations
 * - getAndIncrement() is atomic, ensuring each thread gets a unique number
 * - Base62 encoding is stateless and thread-safe
 * - Suitable for single-instance deployments where distributed uniqueness is not required
 *
 * Simple counter-based URL generator.
 * Suitable for single-node deployments.
 * Works well with single server.
 * Uses 0-9, A-Z, a-z to generate the short URL
 * Converts number to Strings such as 10000000 --> "4C92"
 * Pros:
 * 1. Simple and easy to understand
 * 2. Works well with single server

 * Cons:
 * 1. Not suitable for distributed systems
 * 2. No time information is stored in the short URL
 */
@Component
public class SimpleCounterGenerator implements UrlGeneratorStrategy {
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = CHARACTERS.length();
    private final AtomicLong counter = new AtomicLong(1000000); // Start from 1 million

    @Override
    public String generateShortUrl() {
        long number = counter.getAndIncrement();
        return base62Encode(number);
    }

    private String base62Encode(long number) {
        if (number == 0) {
            return String.valueOf(CHARACTERS.charAt(0));
        }

        StringBuilder shortUrl = new StringBuilder();
        while (number > 0) {
            shortUrl.insert(0, CHARACTERS.charAt((int) (number % BASE)));
            number = number / BASE;
        }
        return shortUrl.toString();
    }
} 