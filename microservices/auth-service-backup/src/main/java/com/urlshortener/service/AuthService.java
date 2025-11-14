package com.urlshortener.service;

import com.urlshortener.dto.AuthResponse;
import com.urlshortener.dto.LoginRequest;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.model.User;

import java.util.Optional;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    Optional<User> findByUsername(String username);
} 