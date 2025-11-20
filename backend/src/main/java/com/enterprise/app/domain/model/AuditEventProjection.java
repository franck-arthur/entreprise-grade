package com.enterprise.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CQRS Query Model - Read side for audit events.
 *
 * This is the read-optimized model used for querying audit events.
 * It's designed for fast queries with multiple indexes and denormalized data.
 * This table is populated asynchronously from the command model.
 *
 * Follows CQRS pattern: this model handles QUERIES (read operations).
 *
 * Key differences from Command model:
 * - Multiple indexes for different query patterns
 * - Denormalized fields for faster queries
 * - Optimized for filtering and aggregations
 * - May include computed/aggregated fields
 */
@Entity
@Table(
    name = "audit_events_projection",
    indexes = {
        @Index(name = "idx_audit_proj_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_proj_user", columnList = "user_id, timestamp"),
        @Index(name = "idx_audit_proj_type", columnList = "event_type, timestamp"),
        @Index(name = "idx_audit_proj_entity", columnList = "target_entity_type, target_entity_id"),
        @Index(name = "idx_audit_proj_success", columnList = "success, timestamp"),
        @Index(name = "idx_audit_proj_date", columnList = "event_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventProjection {

    @Id
    private UUID id; // Same ID as command model

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(name = "event_category", length = 50)
    private String eventCategory; // Denormalized: USER, AUTH, BATCH, SECURITY, SYSTEM

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "target_entity_type", length = 100)
    private String targetEntityType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Column(name = "target_entity_name", length = 200)
    private String targetEntityName; // Denormalized for display

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "success")
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "event_date")
    private java.sql.Date eventDate; // Denormalized for date-based queries

    @Column(name = "event_hour")
    private Integer eventHour; // Denormalized for hourly aggregations

    /**
     * Create projection from command model.
     */
    public static AuditEventProjection fromCommand(AuditEventCommand command) {
        AuditEventProjection projection = AuditEventProjection.builder()
            .id(command.getId())
            .eventType(command.getEventType())
            .eventCategory(getEventCategory(command.getEventType()))
            .userId(command.getUserId())
            .username(command.getUsername())
            .targetEntityType(command.getTargetEntityType())
            .targetEntityId(command.getTargetEntityId())
            .ipAddress(command.getIpAddress())
            .userAgent(command.getUserAgent())
            .details(command.getDetails())
            .success(command.isSuccess())
            .errorMessage(command.getErrorMessage())
            .timestamp(command.getTimestamp())
            .build();

        // Set denormalized fields
        if (command.getTimestamp() != null) {
            projection.setEventDate(java.sql.Date.valueOf(command.getTimestamp().toLocalDate()));
            projection.setEventHour(command.getTimestamp().getHour());
        }

        return projection;
    }

    /**
     * Get event category from event type (for denormalization).
     */
    private static String getEventCategory(AuditEventType eventType) {
        String typeStr = eventType.name();
        if (typeStr.startsWith("USER_")) {
            return "USER";
        } else if (typeStr.startsWith("LOGIN") || typeStr.equals("LOGOUT") || typeStr.equals("PASSWORD_CHANGED")) {
            return "AUTH";
        } else if (typeStr.startsWith("BATCH_IMPORT")) {
            return "BATCH";
        } else if (typeStr.contains("UNAUTHORIZED") || typeStr.contains("FORBIDDEN")) {
            return "SECURITY";
        } else {
            return "SYSTEM";
        }
    }

    /**
     * Update target entity name (denormalization for display).
     */
    public void setTargetEntityNameFromEntity(String name) {
        this.targetEntityName = name;
    }
}
