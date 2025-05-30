package com.urlshortener.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/error")
public class ErrorController {

    @GetMapping("/404")
    public ResponseEntity<String> handleNotFound() {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body("URL not found");
    }

    @GetMapping("/410")
    public ResponseEntity<String> handleGone() {
        return ResponseEntity
            .status(HttpStatus.GONE)
            .body("URL has expired");
    }
} 