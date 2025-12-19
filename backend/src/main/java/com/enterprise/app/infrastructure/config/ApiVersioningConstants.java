package com.enterprise.app.infrastructure.config;

/**
 * Constants for API versioning across the application.
 *
 * Centralizes version definitions to ensure consistency and ease of maintenance.
 */
public final class ApiVersioningConstants {

    private ApiVersioningConstants() {
        // Utility class
    }

    // Base API path
    public static final String API_BASE_PATH = "/api";

    // Version paths
    public static final String V1_PATH = API_BASE_PATH + "/v1";
    public static final String V2_PATH = API_BASE_PATH + "/v2";

    // Media types for header-based versioning
    public static final String V1_MEDIA_TYPE = "application/vnd.enterprise.v1+json";
    public static final String V2_MEDIA_TYPE = "application/vnd.enterprise.v2+json";

    // Headers
    public static final String API_VERSION_HEADER = "API-Version";
    public static final String DEPRECATED_HEADER = "API-Deprecated";
    public static final String SUNSET_HEADER = "Sunset";

    // Version values
    public static final String VERSION_1 = "1";
    public static final String VERSION_2 = "2";

    // Deprecation messages
    public static final String V1_DEPRECATION_MESSAGE =
        "API v1 is deprecated. Please migrate to v2. See documentation for migration guide.";
}