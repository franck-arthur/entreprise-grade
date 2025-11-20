package com.enterprise.app.domain.model;

/**
 * Status enumeration for batch import operations.
 * Tracks the lifecycle of a batch import from pending to completion.
 */
public enum BatchImportStatus {
    /**
     * Import has been created but not yet started.
     */
    PENDING,

    /**
     * Import is currently being processed.
     */
    PROCESSING,

    /**
     * Import has completed successfully (all lines processed).
     */
    COMPLETED,

    /**
     * Import completed with some errors (partial success).
     */
    COMPLETED_WITH_ERRORS,

    /**
     * Import failed completely.
     */
    FAILED,

    /**
     * Import was cancelled by user or system.
     */
    CANCELLED
}
