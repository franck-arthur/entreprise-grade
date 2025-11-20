-- Initial schema migration
-- Creates users table and related structures

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone_number VARCHAR(20),
    keycloak_id VARCHAR(100) UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    last_login_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- User roles table
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_role UNIQUE (user_id, role)
);

-- Indexes for performance
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_user_keycloak_id ON users(keycloak_id);
CREATE INDEX idx_user_active ON users(active);
CREATE INDEX idx_user_created_at ON users(created_at);

-- Comments for documentation
COMMENT ON TABLE users IS 'Core user entities with authentication and profile information';
COMMENT ON TABLE user_roles IS 'User role assignments - many-to-many relationship';

COMMENT ON COLUMN users.id IS 'Unique user identifier (UUID)';
COMMENT ON COLUMN users.username IS 'Unique username for login';
COMMENT ON COLUMN users.email IS 'User email address (unique)';
COMMENT ON COLUMN users.keycloak_id IS 'Keycloak user ID for OAuth2 integration';
COMMENT ON COLUMN users.active IS 'Indicates if user account is active';
COMMENT ON COLUMN users.email_verified IS 'Indicates if email has been verified';
COMMENT ON COLUMN users.version IS 'Optimistic locking version';
