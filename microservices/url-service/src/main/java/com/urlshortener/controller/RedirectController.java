package com.urlshortener.controller;

import com.urlshortener.dto.UrlResponseDto;
import com.urlshortener.exception.UrlDeactivatedException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Controller
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;

    @GetMapping("/{shortUrl}")
    public RedirectView redirectToOriginalUrl(@PathVariable String shortUrl, HttpServletRequest request) {
        try {
            UrlResponseDto urlResponse = urlService.getOriginalUrl(shortUrl);
            String originalUrl = urlResponse.getOriginalUrl();

            // TODO: Analytics tracking will be added in Phase 6 when analytics-service is created
            // Will use Kafka or REST API to track clicks asynchronously

            return new RedirectView(originalUrl);
        } catch (UrlNotFoundException e) {
            throw new UrlNotFoundException("URL not found");
        } catch (UrlExpiredException e) {
            throw new UrlExpiredException("URL has expired");
        } catch (UrlDeactivatedException e) {
            throw new UrlDeactivatedException("URL has been deactivated");
        }
    }
} 