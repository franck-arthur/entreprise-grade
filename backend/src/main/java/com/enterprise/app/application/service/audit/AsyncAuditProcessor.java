package com.enterprise.app.application.service.audit;

import com.enterprise.app.domain.model.AuditEventCommand;
import com.enterprise.app.domain.model.AuditEventProjection;
import com.enterprise.app.domain.port.AuditEventQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncAuditProcessor {
    private final AuditEventQueryPort auditEventQueryPort;

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
            auditEventQueryPort.save(projection);

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

        auditEventQueryPort.save(projection);
    }


}
