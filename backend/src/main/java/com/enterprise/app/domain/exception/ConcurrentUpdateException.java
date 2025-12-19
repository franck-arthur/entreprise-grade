package com.enterprise.app.domain.exception;

/**
 * Exception lancée lorsqu'une modification concurrente est détectée.
 * Utilisée pour gérer les conflits de versioning optimiste.
 */
public class ConcurrentUpdateException extends BusinessException {

    public ConcurrentUpdateException(String message) {
        super(message);
    }

    public ConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}