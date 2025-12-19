package com.enterprise.app.infrastructure.config;

import com.enterprise.app.infrastructure.web.ApiVersioningInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.MediaType;

/**
 * Configuration for API Versioning.
 *
 * Supports multiple versioning strategies:
 * 1. URL Path versioning: /api/v1/users, /api/v2/users
 * 2. Header versioning: Accept: application/vnd.enterprise.v1+json
 * 3. Parameter versioning: /api/users?version=1
 *
 * Current implementation focuses on URL path versioning for clarity and RESTful design.
 */
@Configuration
@RequiredArgsConstructor
public class ApiVersioningConfig implements WebMvcConfigurer {

    private final ApiVersioningInterceptor apiVersioningInterceptor;

    /**
     * Configure content negotiation to support API versioning via Accept headers.
     * This is an alternative to URL path versioning.
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorParameter(false)
            .favorPathExtension(false)
            .ignoreAcceptHeader(false)
            .useRegisteredExtensionsOnly(false)
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("json", MediaType.APPLICATION_JSON)
            // Custom media types for API versioning via Accept header
            .mediaType("v1", MediaType.valueOf("application/vnd.enterprise.v1+json"))
            .mediaType("v2", MediaType.valueOf("application/vnd.enterprise.v2+json"));
    }

    /**
     * Register the API versioning interceptor.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiVersioningInterceptor)
            .addPathPatterns("/api/**");
    }
}