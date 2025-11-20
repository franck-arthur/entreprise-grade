package com.enterprise.app.application.dto.audit;

import com.enterprise.app.domain.model.AuditEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * CQRS Command - Create audit event (Write side).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuditEventCommand {
    private AuditEventType eventType;
    private UUID userId;
    private String username;
    private String targetEntityType;
    private UUID targetEntityId;
    private String targetEntityName;
    private String ipAddress;
    private String userAgent;
    private String details;
    private boolean success;
    private String errorMessage;
}
