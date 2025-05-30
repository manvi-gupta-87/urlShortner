package com.urlshortener.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Standardized error response for all API errors.
 * Provides consistent error format across the application.
 */
@Data
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
} 