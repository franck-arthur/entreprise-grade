package com.enterprise.app.domain.model;

/**
 * User roles enum.
 *
 * Defines the available roles in the system.
 * Must match the roles configured in Keycloak.
 */
public enum Role {
    /**
     * Regular user with basic permissions.
     */
    USER,

    /**
     * Administrator with full system access.
     */
    ADMIN,

    /**
     * Technical lead with elevated permissions.
     */
    TECH_LEAD,

    /**
     * Manager with team management capabilities.
     */
    MANAGER,

    /**
     * System user for automated processes.
     */
    SYSTEM
}
