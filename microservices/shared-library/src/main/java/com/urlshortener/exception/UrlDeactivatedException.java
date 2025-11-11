package com.urlshortener.exception;

public class UrlDeactivatedException extends RuntimeException {
    public UrlDeactivatedException(String message) {
        super(message);
    }
} 