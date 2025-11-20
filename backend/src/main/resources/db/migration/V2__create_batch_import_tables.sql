-- Migration V2: Batch Import Tables
-- Creates tables for batch CSV import with multithreading support

-- Batch imports main table
CREATE TABLE batch_imports (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_lines INTEGER NOT NULL DEFAULT 0,
    processed_lines INTEGER NOT NULL DEFAULT 0,
    success_lines INTEGER NOT NULL DEFAULT 0,
    failed_lines INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    initiated_by_user_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_batch_import_user FOREIGN KEY (initiated_by_user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

-- Batch import lines table (individual CSV lines)
CREATE TABLE batch_import_lines (
    id UUID PRIMARY KEY,
    batch_import_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    username VARCHAR(50),
    email VARCHAR(100),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone_number VARCHAR(20),
    roles VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_user_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    processed_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_batch_line_import FOREIGN KEY (batch_import_id)
        REFERENCES batch_imports(id) ON DELETE CASCADE,
    CONSTRAINT fk_batch_line_user FOREIGN KEY (created_user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for performance optimization
CREATE INDEX idx_batch_import_status ON batch_imports(status);
CREATE INDEX idx_batch_import_user ON batch_imports(initiated_by_user_id);
CREATE INDEX idx_batch_import_created ON batch_imports(created_at DESC);
CREATE INDEX idx_batch_import_completed ON batch_imports(completed_at DESC);

CREATE INDEX idx_batch_line_import ON batch_import_lines(batch_import_id);
CREATE INDEX idx_batch_line_status ON batch_import_lines(status);
CREATE INDEX idx_batch_line_number ON batch_import_lines(batch_import_id, line_number);

-- Check constraints for data integrity
ALTER TABLE batch_imports ADD CONSTRAINT chk_batch_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED', 'CANCELLED'));

ALTER TABLE batch_import_lines ADD CONSTRAINT chk_line_status
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'));

ALTER TABLE batch_imports ADD CONSTRAINT chk_batch_lines_positive
    CHECK (total_lines >= 0 AND processed_lines >= 0 AND success_lines >= 0 AND failed_lines >= 0);

ALTER TABLE batch_imports ADD CONSTRAINT chk_batch_lines_consistency
    CHECK (processed_lines = success_lines + failed_lines);

-- Comments for documentation
COMMENT ON TABLE batch_imports IS 'Main table tracking batch CSV import operations with multithreading support';
COMMENT ON TABLE batch_import_lines IS 'Individual CSV lines from batch imports with processing status';

COMMENT ON COLUMN batch_imports.status IS 'Current status: PENDING, PROCESSING, COMPLETED, COMPLETED_WITH_ERRORS, FAILED, CANCELLED';
COMMENT ON COLUMN batch_imports.total_lines IS 'Total number of lines in the CSV file (excluding header)';
COMMENT ON COLUMN batch_imports.processed_lines IS 'Number of lines processed (success + failed)';
COMMENT ON COLUMN batch_imports.success_lines IS 'Number of successfully imported lines';
COMMENT ON COLUMN batch_imports.failed_lines IS 'Number of failed lines';
COMMENT ON COLUMN batch_imports.started_at IS 'Timestamp when processing started';
COMMENT ON COLUMN batch_imports.completed_at IS 'Timestamp when processing completed (success, error, or cancelled)';

COMMENT ON COLUMN batch_import_lines.line_number IS 'Original line number in the CSV file';
COMMENT ON COLUMN batch_import_lines.status IS 'Processing status: PENDING, SUCCESS, FAILED';
COMMENT ON COLUMN batch_import_lines.created_user_id IS 'Reference to the user created from this line (if successful)';
COMMENT ON COLUMN batch_import_lines.processed_at IS 'Timestamp when this line was processed';
