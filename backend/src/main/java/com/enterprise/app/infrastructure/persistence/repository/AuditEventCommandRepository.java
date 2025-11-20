package com.enterprise.app.infrastructure.persistence.repository;

import com.enterprise.app.domain.model.AuditEventCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * CQRS Command Repository - Write side.
 * Optimized for fast inserts of audit events.
 * No complex queries - that's handled by the Query repository.
 */
@Repository
public interface AuditEventCommandRepository extends JpaRepository<AuditEventCommand, UUID> {
    // Minimal methods - only for writing
    // Complex queries should use AuditEventProjectionRepository
}
