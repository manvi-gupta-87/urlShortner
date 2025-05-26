package com.urlshortener.util;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class UrlShortenerUtil {
    private static final String ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int URL_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    public String generateShortUrl() {
        StringBuilder shortUrl = new StringBuilder(URL_LENGTH);
        for (int i = 0; i < URL_LENGTH; i++) {
            shortUrl.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        }
        return shortUrl.toString();
    }
} 