package com.enterprise.app.domain.model;

/**
 * Types of audit events tracked in the system.
 * Used for both command and query models.
 */
public enum AuditEventType {
    // User management events
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    USER_ACTIVATED,
    USER_DEACTIVATED,

    // Authentication events
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,

    // Batch import events
    BATCH_IMPORT_STARTED,
    BATCH_IMPORT_COMPLETED,
    BATCH_IMPORT_FAILED,

    // Security events
    UNAUTHORIZED_ACCESS,
    FORBIDDEN_ACCESS,
    PASSWORD_CHANGED,

    // System events
    SYSTEM_ERROR,
    CONFIGURATION_CHANGED
}
