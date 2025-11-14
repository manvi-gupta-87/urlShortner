package com.urlshortener.lib;

import com.urlshortener.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", fallback = AuthServiceClientFallback.class)
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{username}")
    User getUserByUsername(@PathVariable String username);
}
