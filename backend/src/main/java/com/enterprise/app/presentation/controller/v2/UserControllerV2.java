package com.enterprise.app.presentation.controller.v2;

import com.enterprise.app.application.dto.CreateUserRequest;
import com.enterprise.app.application.dto.UpdateUserRequest;
import com.enterprise.app.application.dto.UserDTO;
import com.enterprise.app.application.usecase.UserService;
import com.enterprise.app.infrastructure.config.ApiVersioningConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for User management - API Version 2.
 *
 * V2 improvements over V1:
 * - Enhanced response format with metadata
 * - Bulk operations support
 * - Better error handling with detailed error responses
 * - Additional filtering capabilities
 * - Performance optimizations
 * - HATEOAS support for navigation
 */
@RestController
@RequestMapping(ApiVersioningConstants.V2_PATH + "/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users V2", description = "User management API - Version 2")
@SecurityRequirement(name = "bearerAuth")
public class UserControllerV2 {

    private final UserService userService;

    /**
     * Get all users with enhanced filtering and metadata (V2 enhancement).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get all users with enhanced filtering",
        description = "V2: Enhanced version with better filtering, metadata, and performance optimizations"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users with metadata"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getAllUsersV2(
        @Parameter(description = "Filter by active status")
        @RequestParam(required = false) Boolean active,

        @Parameter(description = "Filter by roles (comma-separated)")
        @RequestParam(required = false) List<String> roles,

        @Parameter(description = "Search in username, email, firstName, lastName")
        @RequestParam(required = false) String search,

        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        log.debug("GET /api/v2/users - Enhanced filtering: active={}, roles={}, search={}",
            active, roles, search);

        // V2 enhancement: Use enhanced filtering
        Page<UserDTO> users = userService.getAllUsersWithFilters(active, roles, search, pageable);

        // V2 enhancement: Return structured response with metadata
        Map<String, Object> response = Map.of(
            "data", users.getContent(),
            "pagination", Map.of(
                "page", users.getNumber(),
                "size", users.getSize(),
                "totalElements", users.getTotalElements(),
                "totalPages", users.getTotalPages()
            ),
            "metadata", Map.of(
                "apiVersion", "2.0",
                "timestamp", System.currentTimeMillis(),
                "filters", Map.of(
                    "active", active,
                    "roles", roles,
                    "search", search
                )
            )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID with enhanced response format (V2).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER') or #id == authentication.principal.claims['sub']")
    @Operation(
        summary = "Get user by ID",
        description = "V2: Enhanced response with metadata and related data"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, Object>> getUserByIdV2(
        @Parameter(description = "User ID", required = true)
        @PathVariable UUID id
    ) {
        log.debug("GET /api/v2/users/{}", id);

        UserDTO user = userService.getUserById(id);

        // V2 enhancement: Structured response with metadata
        Map<String, Object> response = Map.of(
            "data", user,
            "metadata", Map.of(
                "apiVersion", "2.0",
                "timestamp", System.currentTimeMillis()
            ),
            "_links", Map.of(
                "self", "/api/v2/users/" + id,
                "update", "/api/v2/users/" + id,
                "delete", "/api/v2/users/" + id,
                "activate", "/api/v2/users/" + id + "/activate",
                "deactivate", "/api/v2/users/" + id + "/deactivate"
            )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Create multiple users in batch (V2 new feature).
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD')")
    @Operation(
        summary = "Create multiple users in batch",
        description = "V2: New feature to create multiple users in a single request"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "207", description = "Multi-status: Some users created, some failed"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, Object>> createUsersInBatch(
        @Parameter(description = "List of users to create", required = true)
        @Valid @RequestBody List<CreateUserRequest> requests
    ) {
        log.info("POST /api/v2/users/batch - Creating {} users", requests.size());

        // V2 feature: Bulk creation with detailed results
        Map<String, Object> results = userService.createUsersInBatch(requests);

        Map<String, Object> response = Map.of(
            "results", results,
            "metadata", Map.of(
                "apiVersion", "2.0",
                "timestamp", System.currentTimeMillis(),
                "batchSize", requests.size()
            )
        );

        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
    }

    /**
     * Create a single user (V2 enhanced).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD')")
    @Operation(
        summary = "Create new user",
        description = "V2: Enhanced version with better validation and response format"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<Map<String, Object>> createUserV2(
        @Parameter(description = "User creation request", required = true)
        @Valid @RequestBody CreateUserRequest request
    ) {
        log.info("POST /api/v2/users - Creating user: {}", request.getUsername());

        UserDTO createdUser = userService.createUser(request);

        // V2 enhancement: Structured response
        Map<String, Object> response = Map.of(
            "data", createdUser,
            "metadata", Map.of(
                "apiVersion", "2.0",
                "timestamp", System.currentTimeMillis(),
                "operation", "create"
            ),
            "_links", Map.of(
                "self", "/api/v2/users/" + createdUser.getId(),
                "update", "/api/v2/users/" + createdUser.getId(),
                "activate", "/api/v2/users/" + createdUser.getId() + "/activate"
            )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update user with partial updates support (V2 enhancement).
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD')")
    @Operation(
        summary = "Partially update user",
        description = "V2: New PATCH endpoint for partial updates (addition to PUT)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, Object>> patchUserV2(
        @Parameter(description = "User ID", required = true)
        @PathVariable UUID id,
        @Parameter(description = "Partial update data", required = true)
        @RequestBody Map<String, Object> partialUpdate
    ) {
        log.info("PATCH /api/v2/users/{} - Partial update", id);

        // V2 feature: Partial updates
        UserDTO updatedUser = userService.partialUpdateUser(id, partialUpdate);

        Map<String, Object> response = Map.of(
            "data", updatedUser,
            "metadata", Map.of(
                "apiVersion", "2.0",
                "timestamp", System.currentTimeMillis(),
                "operation", "partial_update",
                "fieldsUpdated", partialUpdate.keySet()
            )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Full update (maintains V1 compatibility).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD')")
    @Operation(
        summary = "Update user (full replacement)",
        description = "V2: Full update with enhanced response format"
    )
    public ResponseEntity<Map<String, Object>> updateUserV2(
        @Parameter(description = "User ID", required = true)
        @PathVariable UUID id,
        @Parameter(description = "User update request", required = true)
        @Valid @RequestBody UpdateUserRequest request
    ) {
        log.info("PUT /api/v2/users/{}", id);

        UserDTO updatedUser = userService.updateUser(id, request);

        Map<String, Object> response = Map.of(
            "data", updatedUser,
            "metadata", Map.of(
                "apiVersion", "2.0",
                "timestamp", System.currentTimeMillis(),
                "operation", "full_update"
            )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Delete user (enhanced response).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete user",
        description = "V2: Enhanced with confirmation response"
    )
    public ResponseEntity<Map<String, Object>> deleteUserV2(
        @Parameter(description = "User ID", required = true)
        @PathVariable UUID id
    ) {
        log.info("DELETE /api/v2/users/{}", id);

        userService.deleteUser(id);

        Map<String, Object> response = Map.of(
            "message", "User deleted successfully",
            "metadata", Map.of(
                "apiVersion", "2.0",
                "timestamp", System.currentTimeMillis(),
                "operation", "delete",
                "deletedId", id
            )
        );

        return ResponseEntity.ok(response);
    }
}