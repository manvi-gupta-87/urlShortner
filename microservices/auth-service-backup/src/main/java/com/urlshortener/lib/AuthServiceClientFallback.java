package com.urlshortener.lib;

import com.urlshortener.model.User;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceClientFallback implements AuthServiceClient {

    @Override
    public User getUserByUsername(String username) {
        throw new RuntimeException("Auth service is unavailable. Please try again later.");
    }
}
