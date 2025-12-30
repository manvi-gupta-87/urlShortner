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
 * OpenAPI/Swagger Configuration for URL Service.
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
 * │    - Title: "URL Service API"                                               │
 * │    - Version: "1.0.0"                                                       │
 * │    - Description: Detailed explanation of the service                       │
 * │    - Contact info: Team email                                               │
 * │    - Server URLs: Where the API is hosted (dropdown in Swagger UI)          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  ACCESS URLs:                                                               │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  Swagger UI:     http://localhost:8082/swagger-ui.html                      │
 * │  OpenAPI JSON:   http://localhost:8082/api-docs                             │
 * │  OpenAPI YAML:   http://localhost:8082/api-docs.yaml                        │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Visual Result in Swagger UI:
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  URL Service API  v1.0.0                                                    │
 * │  URL shortening and redirection service...                                  │
 * │                                                                             │
 * │  Servers: [localhost:8082 ▼]                                                │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  POST /api/v1/urls           - Create short URL                             │
 * │  GET  /api/v1/urls/{shortUrl} - Get original URL                            │
 * │  DELETE /api/v1/urls/{shortUrl} - Deactivate URL                            │
 * │  GET  /api/v1/urls           - Get user's URLs                              │
 * │  GET  /api/v1/urls/{shortCode}/stats - Get URL statistics                   │
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
    public OpenAPI urlServiceOpenAPI() {
        return new OpenAPI()
                // Info section - appears at the top of Swagger UI
                .info(new Info()
                        .title("URL Service API")  // Main title in Swagger header
                        .description("URL shortening and redirection service for URL Shortener platform. " +
                                "Handles creating short URLs, retrieving original URLs, and URL analytics.")
                        .version("1.0.0")  // API version displayed next to title
                        .contact(new Contact()
                                .name("URL Shortener Team")
                                .email("team@urlshortener.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                // Servers section - dropdown in Swagger UI to select environment
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local Development"),
                        new Server().url("http://url-service:8082").description("Docker Environment")
                ));
    }
}
