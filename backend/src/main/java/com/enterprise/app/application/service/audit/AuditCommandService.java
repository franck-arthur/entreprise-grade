package com.enterprise.app.application.service.audit;

import com.enterprise.app.application.dto.audit.CreateAuditEventCommand;
import com.enterprise.app.domain.model.AuditEventCommand;
import com.enterprise.app.domain.model.AuditEventProjection;
import com.enterprise.app.domain.port.AuditEventCommandPort;
import com.enterprise.app.domain.port.AuditEventQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CQRS Command Service - Handles write operations for audit events.
 *
 * This service implements the COMMAND side of CQRS:
 * - Records events to the command model (write-optimized)
 * - Asynchronously projects events to the query model (read-optimized)
 * - Ensures eventual consistency between command and query models
 *
 * Key CQRS principles:
 * 1. Write operations go to command model first (fast inserts)
 * 2. Projection to query model happens asynchronously
 * 3. Separate models optimized for their specific use cases
 *
 * Follows hexagonal architecture by depending on ports (interfaces)
 * instead of concrete implementations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditCommandService {

    private final AuditEventCommandPort auditEventCommandPort;
    private final AuditEventQueryPort auditEventQueryPort;
    private final AsyncAuditProcessor asyncAuditProcessor;
    /**
     * Record audit event (COMMAND).
     *
     * This is the main write operation. It:
     * 1. Saves to command model (fast write)
     * 2. Triggers async projection to query model
     *
     * @param command The audit event command
     * @return The created event ID
     */
    @Transactional
    public AuditEventCommand recordEvent(CreateAuditEventCommand command) {
        log.debug("Recording audit event: {}", command.getEventType());

        // Create command model (write-optimized)
        AuditEventCommand auditEvent = AuditEventCommand.builder()
            .eventType(command.getEventType())
            .userId(command.getUserId())
            .username(command.getUsername())
            .targetEntityType(command.getTargetEntityType())
            .targetEntityId(command.getTargetEntityId().toString())
            .ipAddress(command.getIpAddress())
            .userAgent(command.getUserAgent())
            .details(command.getDetails())
            .success(command.isSuccess())
            .errorMessage(command.getErrorMessage())
            .build();

        // Save to command model
        AuditEventCommand savedEvent = auditEventCommandPort.save(auditEvent);

        // Project to query model asynchronously (eventual consistency)
        asyncAuditProcessor.projectEventAsync(savedEvent, command.getTargetEntityName());

        log.debug("Audit event recorded with ID: {}", savedEvent.getId());
        return savedEvent;
    }

    /**
     * Rebuild all projections from command model.
     * Use for data recovery or migration.
     */
    @Transactional
    public void rebuildProjections() {
        log.info("Rebuilding all audit event projections");

        // Clear existing projections
        auditEventQueryPort.deleteAll();

        // Rebuild from command model
        auditEventCommandPort.findAll().forEach(command -> {
            AuditEventProjection projection = AuditEventProjection.fromCommand(command);
            auditEventQueryPort.save(projection);
        });

        log.info("Rebuilt {} audit event projections", auditEventQueryPort.count());
    }
}
