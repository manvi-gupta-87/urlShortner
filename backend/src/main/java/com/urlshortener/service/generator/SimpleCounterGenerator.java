package com.urlshortener.service.generator;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple counter-based URL generator.
 * Suitable for single-node deployments.
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