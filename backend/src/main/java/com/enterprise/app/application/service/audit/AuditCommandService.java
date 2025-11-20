package com.enterprise.app.application.service.audit;

import com.enterprise.app.application.dto.audit.CreateAuditEventCommand;
import com.enterprise.app.domain.model.AuditEventCommand;
import com.enterprise.app.domain.model.AuditEventProjection;
import com.enterprise.app.infrastructure.persistence.repository.AuditEventCommandRepository;
import com.enterprise.app.infrastructure.persistence.repository.AuditEventProjectionRepository;
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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditCommandService {

    private final AuditEventCommandRepository commandRepository;
    private final AuditEventProjectionRepository projectionRepository;

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
            .targetEntityId(command.getTargetEntityId())
            .ipAddress(command.getIpAddress())
            .userAgent(command.getUserAgent())
            .details(command.getDetails())
            .success(command.isSuccess())
            .errorMessage(command.getErrorMessage())
            .build();

        // Save to command model
        AuditEventCommand savedEvent = commandRepository.save(auditEvent);

        // Project to query model asynchronously (eventual consistency)
        projectEventAsync(savedEvent, command.getTargetEntityName());

        log.debug("Audit event recorded with ID: {}", savedEvent.getId());
        return savedEvent;
    }

    /**
     * Project event to query model asynchronously.
     *
     * This implements eventual consistency:
     * - Runs in separate thread (@Async)
     * - Creates read-optimized projection
     * - Adds denormalized fields for faster queries
     *
     * Even if projection fails, the event is recorded in command model.
     *
     * @param commandEvent The command model event
     * @param targetEntityName Denormalized target entity name
     */
    @Async("batchImportExecutor")
    @Transactional
    public void projectEventAsync(AuditEventCommand commandEvent, String targetEntityName) {
        try {
            log.debug("Projecting audit event to query model: {}", commandEvent.getId());

            // Create projection from command
            AuditEventProjection projection = AuditEventProjection.fromCommand(commandEvent);

            // Set denormalized fields
            if (targetEntityName != null) {
                projection.setTargetEntityNameFromEntity(targetEntityName);
            }

            // Save to projection (query model)
            projectionRepository.save(projection);

            log.debug("Audit event projected successfully: {}", commandEvent.getId());

        } catch (Exception e) {
            // Log error but don't fail the command
            // This ensures command side always succeeds
            log.error("Failed to project audit event to query model: {}",
                commandEvent.getId(), e);
        }
    }

    /**
     * Synchronously project event (for critical operations).
     * Use sparingly - prefer async projection for better performance.
     */
    @Transactional
    public void projectEventSync(AuditEventCommand commandEvent, String targetEntityName) {
        log.debug("Synchronously projecting audit event: {}", commandEvent.getId());

        AuditEventProjection projection = AuditEventProjection.fromCommand(commandEvent);

        if (targetEntityName != null) {
            projection.setTargetEntityNameFromEntity(targetEntityName);
        }

        projectionRepository.save(projection);
    }

    /**
     * Rebuild all projections from command model.
     * Use for data recovery or migration.
     */
    @Transactional
    public void rebuildProjections() {
        log.info("Rebuilding all audit event projections");

        // Clear existing projections
        projectionRepository.deleteAll();

        // Rebuild from command model
        commandRepository.findAll().forEach(command -> {
            AuditEventProjection projection = AuditEventProjection.fromCommand(command);
            projectionRepository.save(projection);
        });

        log.info("Rebuilt {} audit event projections", projectionRepository.count());
    }
}
