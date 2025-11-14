package com.urlshortener.lib;

import com.urlshortener.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{username}")
    UserDto getUserByUsername(@PathVariable String username);
}
