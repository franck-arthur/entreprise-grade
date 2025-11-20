package com.enterprise.app.domain.exception;

/**
 * Exception thrown when attempting to create a duplicate resource.
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String resourceName, String field, Object value) {
        super(String.format("%s already exists with %s: %s", resourceName, field, value));
    }

    public DuplicateResourceException(String message) {
        super(message);
    }
}
