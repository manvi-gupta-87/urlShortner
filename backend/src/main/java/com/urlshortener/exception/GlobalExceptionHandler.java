package com.urlshortener.exception;

import com.urlshortener.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Global exception handler for the application.
 * Provides consistent error responses and prevents stack trace exposure.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUrlNotFoundException(
            UrlNotFoundException ex, HttpServletRequest request) {
        log.error("URL not found: {}", ex.getMessage());
        return createErrorResponse(
            HttpStatus.NOT_FOUND,
            "Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<ErrorResponse> handleUrlExpiredException(
            UrlExpiredException ex, HttpServletRequest request) {
        log.error("URL expired: {}", ex.getMessage());
        return createErrorResponse(
            HttpStatus.GONE,
            "Gone",
            ex.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler(UrlDeactivatedException.class)
    public ResponseEntity<ErrorResponse> handleUrlDeactivatedException(
            UrlDeactivatedException ex, HttpServletRequest request) {
        log.error("URL deactivated: {}", ex.getMessage());
        return createErrorResponse(
            HttpStatus.GONE,
            "Gone",
            ex.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        
        log.error("Validation error: {}", message);
        return createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            message,
            request.getRequestURI()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .findFirst()
                .orElse("Validation failed");
        
        log.error("Constraint violation: {}", message);
        return createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            message,
            request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        return createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI()
        );
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(
            HttpStatus status, String error, String message, String path) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .build();
        
        return new ResponseEntity<>(errorResponse, status);
    }
} 