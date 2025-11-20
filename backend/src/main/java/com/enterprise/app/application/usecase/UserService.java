package com.enterprise.app.application.usecase;

import com.enterprise.app.application.dto.CreateUserRequest;
import com.enterprise.app.application.dto.UpdateUserRequest;
import com.enterprise.app.application.dto.UserDTO;
import com.enterprise.app.application.mapper.UserMapper;
import com.enterprise.app.domain.exception.DuplicateResourceException;
import com.enterprise.app.domain.exception.ResourceNotFoundException;
import com.enterprise.app.domain.model.Role;
import com.enterprise.app.domain.model.User;
import com.enterprise.app.domain.repository.UserRepository;
import com.enterprise.app.infrastructure.keycloak.KeycloakUserAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User service - Application layer use case.
 *
 * Orchestrates business logic for user management.
 * Uses Resilience4j for circuit breaker, retry, and rate limiting.
 * Implements caching with Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final KeycloakUserAdapter keycloakAdapter;
    private final UserMapper userMapper;

    /**
     * Get all users with pagination.
     */
    @Cacheable(value = "users", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        log.debug("Fetching users - page: {}, size: {}",
            pageable.getPageNumber(), pageable.getPageSize());

        return userRepository.findAll(pageable)
            .map(userMapper::toDTO);
    }

    /**
     * Get user by ID.
     */
    @Cacheable(value = "user", key = "#id")
    public UserDTO getUserById(UUID id) {
        log.debug("Fetching user by id: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        return userMapper.toDTO(user);
    }

    /**
     * Get user by username.
     */
    @Cacheable(value = "user", key = "#username")
    public UserDTO getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        return userMapper.toDTO(user);
    }

    /**
     * Get user entity by username (for internal use).
     */
    public User getUserEntityByUsername(String username) {
        log.debug("Fetching user entity by username: {}", username);

        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Get user by email.
     */
    @Cacheable(value = "user", key = "#email")
    public UserDTO getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return userMapper.toDTO(user);
    }

    /**
     * Create a new user.
     *
     * Steps:
     * 1. Validate uniqueness (email, username)
     * 2. Create user in database
     * 3. Create user in Keycloak
     * 4. Update user with Keycloak ID
     *
     * Uses Circuit Breaker for Keycloak integration.
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    @CircuitBreaker(name = "keycloak", fallbackMethod = "createUserFallback")
    @Retry(name = "keycloak")
    @RateLimiter(name = "userCreation")
    public UserDTO createUser(CreateUserRequest request) {
        log.info("Creating user: {}", request.getUsername());

        // Validate uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        // Set default role if not provided
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            Set<Role> defaultRoles = new HashSet<>();
            defaultRoles.add(Role.USER);
            request.setRoles(defaultRoles);
        }

        // Create user entity
        User user = userMapper.toEntity(request);

        // Save to database first
        user = userRepository.save(user);
        log.debug("User saved to database: {}", user.getId());

        // Create in Keycloak
        String keycloakId = keycloakAdapter.createUser(user, request.getPassword());
        user.setKeycloakId(keycloakId);

        // Update with Keycloak ID
        user = userRepository.save(user);

        log.info("User created successfully: {} (Keycloak ID: {})",
            user.getUsername(), keycloakId);

        return userMapper.toDTO(user);
    }

    /**
     * Fallback method for createUser when Keycloak is unavailable.
     */
    @SuppressWarnings("unused")
    private UserDTO createUserFallback(CreateUserRequest request, Exception e) {
        log.error("Keycloak unavailable, creating user without Keycloak integration", e);

        User user = userMapper.toEntity(request);
        user = userRepository.save(user);

        log.warn("User created without Keycloak integration: {}", user.getUsername());

        return userMapper.toDTO(user);
    }

    /**
     * Update user.
     */
    @Transactional
    @CacheEvict(value = {"user", "users"}, allEntries = true)
    @CircuitBreaker(name = "keycloak")
    public UserDTO updateUser(UUID id, UpdateUserRequest request) {
        log.info("Updating user: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User", "email", request.getEmail());
            }
        }

        // Update entity
        userMapper.updateEntityFromDTO(request, user);

        // Save to database
        user = userRepository.save(user);

        // Update in Keycloak if exists
        if (user.getKeycloakId() != null) {
            keycloakAdapter.updateUser(user.getKeycloakId(), user);
        }

        log.info("User updated successfully: {}", id);

        return userMapper.toDTO(user);
    }

    /**
     * Delete user.
     */
    @Transactional
    @CacheEvict(value = {"user", "users"}, allEntries = true)
    @CircuitBreaker(name = "keycloak")
    public void deleteUser(UUID id) {
        log.info("Deleting user: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Delete from Keycloak first
        if (user.getKeycloakId() != null) {
            keycloakAdapter.deleteUser(user.getKeycloakId());
        }

        // Delete from database
        userRepository.deleteById(id);

        log.info("User deleted successfully: {}", id);
    }

    /**
     * Activate user.
     */
    @Transactional
    @CacheEvict(value = {"user", "users"}, allEntries = true)
    @CircuitBreaker(name = "keycloak")
    public UserDTO activateUser(UUID id) {
        log.info("Activating user: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.activate();
        user = userRepository.save(user);

        // Update in Keycloak
        if (user.getKeycloakId() != null) {
            keycloakAdapter.setUserEnabled(user.getKeycloakId(), true);
        }

        log.info("User activated: {}", id);

        return userMapper.toDTO(user);
    }

    /**
     * Deactivate user.
     */
    @Transactional
    @CacheEvict(value = {"user", "users"}, allEntries = true)
    @CircuitBreaker(name = "keycloak")
    public UserDTO deactivateUser(UUID id) {
        log.info("Deactivating user: {}", id);

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.deactivate();
        user = userRepository.save(user);

        // Update in Keycloak
        if (user.getKeycloakId() != null) {
            keycloakAdapter.setUserEnabled(user.getKeycloakId(), false);
        }

        log.info("User deactivated: {}", id);

        return userMapper.toDTO(user);
    }

    /**
     * Count total users.
     */
    public long countUsers() {
        return userRepository.count();
    }

    /**
     * Count active users.
     */
    public long countActiveUsers() {
        return userRepository.countActive();
    }
}
