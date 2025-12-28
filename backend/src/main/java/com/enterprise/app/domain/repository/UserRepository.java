package com.enterprise.app.domain.repository;

import com.enterprise.app.domain.model.Role;
import com.enterprise.app.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;

/**
 * User repository port (interface).
 *
 * This is a port in hexagonal architecture - defines the contract
 * without knowing the implementation details (JPA, MongoDB, etc.).
 *
 * The actual implementation will be in the infrastructure layer.
 */
public interface UserRepository {

    /**
     * Find user by ID.
     */
    Optional<User> findById(Long id);

    /**
     * Find user by username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by Keycloak ID.
     */
    Optional<User> findByKeycloakId(String keycloakId);

    /**
     * Find all users with pagination.
     */
    Page<User> findAll(Pageable pageable);

    /**
     * Find all active users with pagination.
     */
    Page<User> findAllActive(Pageable pageable);

    /**
     * Save or update user.
     */
    User save(User user);

    /**
     * Delete user by ID.
     */
    void deleteById(Long id);

    /**
     * Check if user exists by email.
     */
    boolean existsByEmail(String email);

    /**
     * Check if user exists by username.
     */
    boolean existsByUsername(String username);

    /**
     * Count total users.
     */
    long count();

    /**
     * Count active users.
     */
    long countActive();

    /**
     * Find users with filters (for V2 API).
     */
    Page<User> findWithFilters(Boolean active, Set<Role> roles, String search, Pageable pageable);
}
