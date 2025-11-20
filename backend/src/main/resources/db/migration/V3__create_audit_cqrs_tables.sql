-- Migration V3: CQRS Audit System Tables
-- Implements Command Query Responsibility Segregation pattern
-- Separate tables for write-optimized commands and read-optimized queries

-- ============================================================
-- COMMAND MODEL (Write-Optimized)
-- ============================================================
-- Minimal indexes, optimized for fast INSERT operations
-- Immutable append-only event log
CREATE TABLE audit_events_command (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id UUID,
    username VARCHAR(50),
    target_entity_type VARCHAR(50),
    target_entity_id UUID,
    description TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    metadata JSONB,
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT chk_audit_cmd_event_type CHECK (event_type IN (
        'USER_CREATED', 'USER_UPDATED', 'USER_DELETED', 'USER_ACTIVATED', 'USER_DEACTIVATED',
        'LOGIN_SUCCESS', 'LOGIN_FAILED', 'LOGOUT', 'PASSWORD_CHANGED',
        'BATCH_IMPORT_STARTED', 'BATCH_IMPORT_COMPLETED', 'BATCH_IMPORT_FAILED',
        'UNAUTHORIZED_ACCESS', 'FORBIDDEN_ACCESS', 'SYSTEM_ERROR', 'CONFIGURATION_CHANGED'
    ))
);

-- Single index on timestamp for chronological append operations
CREATE INDEX idx_audit_cmd_timestamp ON audit_events_command(timestamp DESC);

COMMENT ON TABLE audit_events_command IS 'CQRS Command Model: Write-optimized immutable event log with minimal indexes for fast INSERT';
COMMENT ON COLUMN audit_events_command.event_type IS 'Type of audit event (16 predefined types)';
COMMENT ON COLUMN audit_events_command.metadata IS 'Additional event data stored as JSON for flexibility';
COMMENT ON COLUMN audit_events_command.timestamp IS 'Event occurrence timestamp (indexed for chronological queries)';

-- ============================================================
-- QUERY MODEL (Read-Optimized)
-- ============================================================
-- Multiple indexes and denormalized fields for fast SELECT operations
-- Eventual consistency via async projection from command model
CREATE TABLE audit_events_projection (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    user_id UUID,
    username VARCHAR(50),
    target_entity_type VARCHAR(50),
    target_entity_id UUID,
    target_entity_name VARCHAR(255),
    description TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    metadata JSONB,
    timestamp TIMESTAMP NOT NULL,
    event_date DATE NOT NULL,
    event_hour INTEGER NOT NULL,
    CONSTRAINT chk_audit_proj_event_type CHECK (event_type IN (
        'USER_CREATED', 'USER_UPDATED', 'USER_DELETED', 'USER_ACTIVATED', 'USER_DEACTIVATED',
        'LOGIN_SUCCESS', 'LOGIN_FAILED', 'LOGOUT', 'PASSWORD_CHANGED',
        'BATCH_IMPORT_STARTED', 'BATCH_IMPORT_COMPLETED', 'BATCH_IMPORT_FAILED',
        'UNAUTHORIZED_ACCESS', 'FORBIDDEN_ACCESS', 'SYSTEM_ERROR', 'CONFIGURATION_CHANGED'
    )),
    CONSTRAINT chk_audit_proj_category CHECK (event_category IN (
        'USER', 'AUTH', 'BATCH', 'SECURITY', 'SYSTEM'
    )),
    CONSTRAINT chk_audit_proj_hour CHECK (event_hour >= 0 AND event_hour <= 23)
);

-- 7 indexes for optimal query performance (vs 1 on command model)
CREATE INDEX idx_audit_proj_timestamp ON audit_events_projection(timestamp DESC);
CREATE INDEX idx_audit_proj_user ON audit_events_projection(user_id, timestamp DESC);
CREATE INDEX idx_audit_proj_type ON audit_events_projection(event_type, timestamp DESC);
CREATE INDEX idx_audit_proj_entity ON audit_events_projection(target_entity_type, target_entity_id);
CREATE INDEX idx_audit_proj_success ON audit_events_projection(success, timestamp DESC);
CREATE INDEX idx_audit_proj_date ON audit_events_projection(event_date DESC);
CREATE INDEX idx_audit_proj_category ON audit_events_projection(event_category, timestamp DESC);

-- Composite index for common query patterns
CREATE INDEX idx_audit_proj_user_type_date ON audit_events_projection(user_id, event_type, event_date DESC);

-- GIN index for JSONB metadata queries
CREATE INDEX idx_audit_proj_metadata ON audit_events_projection USING GIN (metadata);

COMMENT ON TABLE audit_events_projection IS 'CQRS Query Model: Read-optimized projection with 7+ indexes and denormalized fields for fast SELECT';
COMMENT ON COLUMN audit_events_projection.event_category IS 'Denormalized category derived from event_type (USER, AUTH, BATCH, SECURITY, SYSTEM)';
COMMENT ON COLUMN audit_events_projection.target_entity_name IS 'Denormalized entity name for display without JOIN';
COMMENT ON COLUMN audit_events_projection.event_date IS 'Denormalized date extracted from timestamp for date-based queries';
COMMENT ON COLUMN audit_events_projection.event_hour IS 'Denormalized hour (0-23) for hourly aggregations';

-- ============================================================
-- PERFORMANCE COMPARISON
-- ============================================================
-- Command Model: 1 index → Fast WRITE (INSERT ~1-2ms)
-- Query Model: 7+ indexes → Fast READ (SELECT with filters ~5-10ms)
-- Eventual Consistency: Async projection (~100-200ms delay acceptable)
