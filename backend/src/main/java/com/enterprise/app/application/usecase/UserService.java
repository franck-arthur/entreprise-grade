package com.enterprise.app.application.usecase;

import com.enterprise.app.application.dto.CreateUserRequest;
import com.enterprise.app.application.dto.UpdateUserRequest;
import com.enterprise.app.application.dto.UserDTO;
import com.enterprise.app.application.mapper.UserMapper;
import com.enterprise.app.application.service.audit.AuditService;
import com.enterprise.app.domain.exception.DuplicateResourceException;
import com.enterprise.app.domain.exception.ResourceNotFoundException;
import com.enterprise.app.domain.model.Role;
import com.enterprise.app.domain.model.User;
import com.enterprise.app.domain.port.ExternalUserManagementPort;
import com.enterprise.app.domain.repository.UserRepository;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * User service - Application layer use case.
 *
 * Orchestrates business logic for user management.
 * Uses Resilience4j for circuit breaker, retry, and rate limiting.
 * Implements caching with Redis.
 *
 * Follows hexagonal architecture by depending on ports (interfaces)
 * instead of concrete implementations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ExternalUserManagementPort externalUserManagement;
    private final UserMapper userMapper;
    private final AuditService auditService;

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
        String keycloakId = externalUserManagement.createUser(user, request.getPassword());
        user.setKeycloakId(keycloakId);

        // Update with Keycloak ID
        user = userRepository.save(user);
        auditService.auditUserCreation(null, user, true);

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
        final User userCreated = userRepository.save(user);

        // Update in Keycloak if exists
        if (userCreated.getKeycloakId() != null) {
            externalUserManagement.updateUser(userCreated.getKeycloakId(), userCreated);
        }
        auditService.auditUserCreation(user, userCreated, true);

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
            externalUserManagement.deleteUser(user.getKeycloakId());
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
            externalUserManagement.setUserEnabled(user.getKeycloakId(), true);
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
            externalUserManagement.setUserEnabled(user.getKeycloakId(), false);
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

    /**
     * Get all users with enhanced filtering (V2 API).
     */
    @Cacheable(value = "users", key = "#active + '-' + #roles + '-' + #search + '-' + #pageable.pageNumber")
    public Page<UserDTO> getAllUsersWithFilters(Boolean active, List<String> roles, String search, Pageable pageable) {
        log.debug("Fetching users with filters - active: {}, roles: {}, search: {}", active, roles, search);

        // Convert string roles to Role enum
        Set<Role> roleSet = null;
        if (roles != null && !roles.isEmpty()) {
            roleSet = roles.stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());
        }

        return userRepository.findWithFilters(active, roleSet, search, pageable)
            .map(userMapper::toDTO);
    }

    /**
     * Create multiple users in batch (V2 API).
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public Map<String, Object> createUsersInBatch(List<CreateUserRequest> requests) {
        log.info("Creating {} users in batch", requests.size());

        List<UserDTO> successfullyCreated = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            CreateUserRequest request = requests.get(i);
            try {
                UserDTO createdUser = createUser(request);
                successfullyCreated.add(createdUser);
            } catch (Exception e) {
                Map<String, Object> failure = Map.of(
                    "index", i,
                    "username", request.getUsername(),
                    "error", e.getMessage(),
                    "errorType", e.getClass().getSimpleName()
                );
                failures.add(failure);
                log.warn("Failed to create user {} in batch: {}", request.getUsername(), e.getMessage());
            }
        }

        return Map.of(
            "successful", successfullyCreated,
            "failed", failures,
            "totalRequested", requests.size(),
            "successCount", successfullyCreated.size(),
            "failureCount", failures.size()
        );
    }

    /**
     * Partially update user (V2 API).
     */
    @Transactional
    @CacheEvict(value = {"user", "users"}, allEntries = true)
    @CircuitBreaker(name = "keycloak")
    public UserDTO partialUpdateUser(UUID id, Map<String, Object> partialUpdate) {
        log.info("Partially updating user: {} with fields: {}", id, partialUpdate.keySet());

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Apply partial updates
        partialUpdate.forEach((field, value) -> {
            switch (field) {
                case "firstName":
                    if (value != null) user.setFirstName(value.toString());
                    break;
                case "lastName":
                    if (value != null) user.setLastName(value.toString());
                    break;
                case "email":
                    if (value != null) {
                        String newEmail = value.toString();
                        if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                            throw new DuplicateResourceException("User", "email", newEmail);
                        }
                        user.setEmail(newEmail);
                    }
                    break;
                case "phoneNumber":
                    if (value != null) user.setPhoneNumber(value.toString());
                    break;
                case "roles":
                    if (value instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> roleStrings = (List<String>) value;
                        Set<Role> newRoles = roleStrings.stream()
                            .map(Role::valueOf)
                            .collect(Collectors.toSet());
                        user.setRoles(newRoles);
                    }
                    break;
                case "active":
                    if (value instanceof Boolean) {
                        Boolean isActive = (Boolean) value;
                        if (isActive) {
                            user.activate();
                        } else {
                            user.deactivate();
                        }
                    }
                    break;
                default:
                    log.warn("Unknown field for partial update: {}", field);
            }
        });

        // Save to database
        user = userRepository.save(user);

        // Update in Keycloak if exists
        if (user.getKeycloakId() != null) {
            externalUserManagement.updateUser(user.getKeycloakId(), user);
        }

        log.info("User partially updated successfully: {}", id);

        return userMapper.toDTO(user);
    }
}
