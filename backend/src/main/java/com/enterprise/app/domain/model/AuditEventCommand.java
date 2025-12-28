package com.enterprise.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * CQRS Command Model - Write side for audit events.
 *
 * This is the write-optimized model used for recording audit events.
 * It's designed for fast inserts with minimal constraints and indexes.
 * Events are immutable once created (no updates or deletes).
 *
 * Follows CQRS pattern: this model handles COMMANDS (write operations).
 */
@Entity
@Table(
    name = "audit_event_commands",
    indexes = {
        @Index(name = "idx_audit_cmd_timestamp", columnList = "timestamp")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "target_entity_type", length = 100)
    private String targetEntityType;

    @Column(name = "target_entity_id")
    private String targetEntityId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "success")
    @Builder.Default
    private boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * Factory method to create audit event for user operations.
     */
    public static AuditEventCommand forUserOperation(
        AuditEventType eventType,
        Long userId,
        String username,
        String targetUserId,
        boolean success
    ) {
        return AuditEventCommand.builder()
            .eventType(eventType)
            .userId(userId)
            .username(username)
            .targetEntityType("User")
            .targetEntityId(targetUserId)
            .success(success)
            .build();
    }

    /**
     * Factory method to create audit event for authentication.
     */
    public static AuditEventCommand forAuthentication(
        AuditEventType eventType,
        String username,
        String ipAddress,
        boolean success,
        String errorMessage
    ) {
        return AuditEventCommand.builder()
            .eventType(eventType)
            .username(username)
            .ipAddress(ipAddress)
            .success(success)
            .errorMessage(errorMessage)
            .build();
    }
}
