package com.enterprise.app.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, UUID id) {
        super(String.format("%s not found with id: %s", resourceName, id));
    }

    public ResourceNotFoundException(String resourceName, String field, Object value) {
        super(String.format("%s not found with %s: %s", resourceName, field, value));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
