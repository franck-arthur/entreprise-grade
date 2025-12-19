package com.enterprise.app.infrastructure.web;

import com.enterprise.app.infrastructure.config.ApiVersioningConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for API versioning handling.
 *
 * Adds deprecation headers and version information to responses.
 * Logs API usage for monitoring and analytics.
 */
@Component
@Slf4j
public class ApiVersioningInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestURI = request.getRequestURI();
        String version = extractVersionFromPath(requestURI);

        if (version != null) {
            // Add version header to response
            response.setHeader(ApiVersioningConstants.API_VERSION_HEADER, version);

            // Add deprecation warning for v1
            if (ApiVersioningConstants.VERSION_1.equals(version)) {
                response.setHeader(ApiVersioningConstants.DEPRECATED_HEADER, "true");
                response.setHeader("Warning", "299 - \"" + ApiVersioningConstants.V1_DEPRECATION_MESSAGE + "\"");
            }

            // Log API usage for monitoring
            log.debug("API v{} accessed: {} {}", version, request.getMethod(), requestURI);
        }

        return true;
    }

    /**
     * Extract version from request path.
     *
     * @param requestURI the request URI
     * @return the version string or null if not found
     */
    private String extractVersionFromPath(String requestURI) {
        if (requestURI.startsWith(ApiVersioningConstants.V1_PATH)) {
            return ApiVersioningConstants.VERSION_1;
        } else if (requestURI.startsWith(ApiVersioningConstants.V2_PATH)) {
            return ApiVersioningConstants.VERSION_2;
        }
        return null;
    }
}