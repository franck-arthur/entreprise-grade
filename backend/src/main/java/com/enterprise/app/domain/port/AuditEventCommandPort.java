package com.enterprise.app.domain.port;

import com.enterprise.app.domain.model.AuditEventCommand;

import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for audit event command operations (CQRS WRITE side).
 * Follows hexagonal architecture pattern.
 *
 * This port defines the contract for write operations on audit events
 * without coupling the domain to any specific implementation.
 * Optimized for fast inserts.
 */
public interface AuditEventCommandPort {

    /**
     * Save an audit event command (write operation).
     *
     * @param auditEventCommand The audit event to save
     * @return The saved audit event
     */
    AuditEventCommand save(AuditEventCommand auditEventCommand);

    /**
     * Find an audit event command by ID.
     *
     * @param id The audit event ID
     * @return The audit event if found
     */
    Optional<AuditEventCommand> findById(UUID id);
}
