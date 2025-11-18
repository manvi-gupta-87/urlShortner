package com.urlshortener.gateway.config;

import com.urlshortener.gateway.filter.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Autowired
    private AuthenticationFilter authFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes (no authentication needed)
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .uri("lb://AUTH-SERVICE"))

                // URL Service Routes (authentication required)
                .route("url-service", r -> r
                        .path("/api/urls/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://URL-SERVICE"))

                // Analytics Service Routes (authentication required)
                .route("analytics-service", r -> r
                        .path("/api/analytics/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://ANALYTICS-SERVICE"))

                // Redirect route (no authentication - public)
                .route("redirect-service", r -> r
                        .path("/{shortCode}")
                        .uri("lb://URL-SERVICE"))

                .build();
    }
}
