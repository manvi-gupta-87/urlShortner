package com.urlshortener.controller;

import com.urlshortener.exception.UrlDeactivatedException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for handling URL redirections.
 * 
 * RedirectView is a Spring MVC class that:
 * - Handles HTTP redirects (302 Found, 301 Moved Permanently)
 * - Redirects the client's browser to a different URL
 * - Maintains proper HTTP status codes and headers
 * - Supports both relative and absolute URLs
 * - Integrates with Spring's view resolution system
 */
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;

    @GetMapping("/{shortCode}")
    public RedirectView redirectToOriginalUrl(@PathVariable String shortCode) {
        try {
            String originalUrl = urlService.getOriginalUrl(shortCode).getOriginalUrl();
            RedirectView redirectView = new RedirectView(originalUrl);
            redirectView.setStatusCode(HttpStatus.FOUND);
            return redirectView;
        } catch (UrlNotFoundException e) {
            RedirectView redirectView = new RedirectView("/error/404");
            redirectView.setStatusCode(HttpStatus.NOT_FOUND);
            return redirectView;
        } catch (UrlExpiredException e) {
            RedirectView redirectView = new RedirectView("/error/410");
            redirectView.setStatusCode(HttpStatus.GONE);
            return redirectView;
        } catch (UrlDeactivatedException e) {
            RedirectView redirectView = new RedirectView("/error/410");
            redirectView.setStatusCode(HttpStatus.GONE);
            return redirectView;
        }
    }
} 