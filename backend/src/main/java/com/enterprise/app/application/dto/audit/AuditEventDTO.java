package com.enterprise.app.application.dto.audit;

import com.enterprise.app.domain.model.AuditEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for audit event responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDTO {
    private Long id;
    private AuditEventType eventType;
    private String eventCategory;
    private Long userId;
    private String username;
    private String targetEntityType;
    private Long targetEntityId;
    private String targetEntityName;
    private String ipAddress;
    private String userAgent;
    private String details;
    private boolean success;
    private String errorMessage;
    private LocalDateTime timestamp;
}
