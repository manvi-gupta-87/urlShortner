package com.urlshortener.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling error responses.
 * Provides consistent error responses for different HTTP status codes.
 */
@RestController
@RequestMapping("/error")
public class ErrorController {

    @GetMapping("/404")
    public ResponseEntity<Map<String, String>> handleNotFound() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "404");
        response.put("error", "Not Found");
        response.put("message", "The requested URL was not found");
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @GetMapping("/410")
    public ResponseEntity<Map<String, String>> handleGone() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "410");
        response.put("error", "Gone");
        response.put("message", "The requested URL is no longer available");
        return new ResponseEntity<>(response, HttpStatus.GONE);
    }
} 