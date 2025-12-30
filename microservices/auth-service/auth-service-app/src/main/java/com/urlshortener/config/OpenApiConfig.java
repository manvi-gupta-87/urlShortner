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
 * OpenAPI/Swagger Configuration for Auth Service.
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
 * │    - Title: "Auth Service API"                                              │
 * │    - Version: "1.0.0"                                                       │
 * │    - Description: Detailed explanation of the service                       │
 * │    - Contact info: Team email                                               │
 * │    - Server URLs: Where the API is hosted (dropdown in Swagger UI)          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  ACCESS URLs:                                                               │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  Swagger UI:     http://localhost:8081/swagger-ui.html                      │
 * │  OpenAPI JSON:   http://localhost:8081/api-docs                             │
 * │  OpenAPI YAML:   http://localhost:8081/api-docs.yaml                        │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Visual Result in Swagger UI:
 * ┌─────────────────────────────────────────────────┐
 * │  Auth Service API  v1.0.0                       │
 * │  Authentication and Authorization service...    │
 * │                                                 │
 * │  Servers: [localhost:8081 ▼]                    │
 * ├─────────────────────────────────────────────────┤
 * │  POST /api/v1/auth/register                     │
 * │  POST /api/v1/auth/login                        │
 * │  GET  /api/v1/auth/users/{username}             │
 * └─────────────────────────────────────────────────┘
 */
@Configuration  // Tells Spring this is a configuration class
public class OpenApiConfig {

    /**
     * Creates and configures the OpenAPI documentation bean.
     *
     * @return OpenAPI object with all metadata configured
     */
    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                // Info section - appears at the top of Swagger UI
                .info(new Info()
                        .title("Auth Service API")  // Main title in Swagger header
                        .description("Authentication and Authorization service for URL Shortener. " +
                                "Handles user registration, login, and JWT token management.")
                        .version("1.0.0")  // API version displayed next to title
                        .contact(new Contact()
                                .name("URL Shortener Team")
                                .email("team@urlshortener.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                // Servers section - dropdown in Swagger UI to select environment
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local Development"),
                        new Server().url("http://auth-service:8081").description("Docker Environment")
                ));
    }
}
