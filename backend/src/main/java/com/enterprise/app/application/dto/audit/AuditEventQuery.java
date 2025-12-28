package com.enterprise.app.application.dto.audit;

import com.enterprise.app.domain.model.AuditEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CQRS Query - Search audit events (Read side).
 * Supports multiple filter criteria for complex queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventQuery {
    private List<AuditEventType> eventTypes;
    private String eventCategory;
    private Long userId;
    private String username;
    private String targetEntityType;
    private Long targetEntityId;
    private Boolean success;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String ipAddress;
}
