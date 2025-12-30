package com.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration for Analytics Service.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  WHAT IS THIS FILE?                                                         │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  This configuration class provides METADATA for your API documentation.     │
 * │  It customizes what appears in the Swagger UI header and description.       │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  WHY IS IT NEEDED?                                                          │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  Without this file, Swagger UI shows:                                       │
 * │    - Title: "OpenAPI definition" (generic, unhelpful)                       │
 * │    - Version: blank                                                         │
 * │    - Description: blank                                                     │
 * │                                                                             │
 * │  With this file, Swagger UI shows:                                          │
 * │    - Title: "Analytics Service API"                                         │
 * │    - Version: "1.0.0"                                                       │
 * │    - Description: Detailed explanation of the service                       │
 * │    - Contact info: Team email                                               │
 * │    - Server URLs: Where the API is hosted (dropdown in Swagger UI)          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  ACCESS URLs:                                                               │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  Swagger UI:     http://localhost:8083/swagger-ui.html                      │
 * │  OpenAPI JSON:   http://localhost:8083/api-docs                             │
 * │  OpenAPI YAML:   http://localhost:8083/api-docs.yaml                        │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Visual Result in Swagger UI:
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  Analytics Service API  v1.0.0                                              │
 * │  Click tracking and analytics service...                                    │
 * │                                                                             │
 * │  Servers: [localhost:8083 ▼]                                                │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  GET /api/v1/analytics/urls/{urlId}/clicks - Get click events               │
 * │  GET /api/v1/analytics/urls/{urlId}/total-clicks - Get total clicks         │
 * │  GET /api/v1/analytics/urls/{urlId}/clicks-by-country - Clicks by country   │
 * │  GET /api/v1/analytics/urls/{urlId}/clicks-by-browser - Clicks by browser   │
 * │  GET /api/v1/analytics/urls/{urlId}/clicks-by-device - Clicks by device     │
 * │  GET /api/v1/analytics/urls/stats - Get URL analytics summary               │
 * └─────────────────────────────────────────────────────────────────────────────┘
 */
@Configuration  // Tells Spring this is a configuration class
public class OpenApiConfig {

    /**
     * Creates and configures the OpenAPI documentation bean.
     *
     * @return OpenAPI object with all metadata configured
     */
    @Bean
    public OpenAPI analyticsServiceOpenAPI() {
        return new OpenAPI()
                // Info section - appears at the top of Swagger UI
                .info(new Info()
                        .title("Analytics Service API")  // Main title in Swagger header
                        .description("Click tracking and analytics service for URL Shortener platform. " +
                                "Tracks URL clicks and provides analytics data including geographic location, " +
                                "browser type, device type, and time-series click data.")
                        .version("1.0.0")  // API version displayed next to title
                        .contact(new Contact()
                                .name("URL Shortener Team")
                                .email("team@urlshortener.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                // Servers section - dropdown in Swagger UI to select environment
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Local Development"),
                        new Server().url("http://analytics-service:8083").description("Docker Environment")
                ));
    }
}
