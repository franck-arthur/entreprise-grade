package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.CreateUserRequest;
import com.enterprise.app.application.dto.UpdateUserRequest;
import com.enterprise.app.application.dto.UserDTO;
import com.enterprise.app.application.usecase.UserService;
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

import java.util.UUID;

/**
 * REST Controller for User management.
 *
 * Provides CRUD operations for users with role-based access control.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management API")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    /**
     * Get all users with pagination.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get all users",
        description = "Retrieve a paginated list of all users. Requires ADMIN, TECH_LEAD, or MANAGER role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<Page<UserDTO>> getAllUsers(
        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        log.debug("GET /api/v1/users - page: {}, size: {}",
            pageable.getPageNumber(), pageable.getPageSize());

        Page<UserDTO> users = userService.getAllUsers(pageable);

        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER') or #id == authentication.principal.claims['sub']")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve a single user by their ID. Users can access their own data."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDTO> getUserById(
        @Parameter(description = "User ID", required = true)
        @PathVariable UUID id
    ) {
        log.debug("GET /api/v1/users/{}", id);

        UserDTO user = userService.getUserById(id);

        return ResponseEntity.ok(user);
    }

    /**
     * Get user by username.
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get user by username",
        description = "Retrieve a user by their username. Requires elevated permissions."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDTO> getUserByUsername(
        @Parameter(description = "Username", required = true)
        @PathVariable String username
    ) {
        log.debug("GET /api/v1/users/username/{}", username);

        UserDTO user = userService.getUserByUsername(username);

        return ResponseEntity.ok(user);
    }

    /**
     * Create a new user.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD')")
    @Operation(
        summary = "Create new user",
        description = "Create a new user. Requires ADMIN or TECH_LEAD role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<UserDTO> createUser(
        @Parameter(description = "User creation request", required = true)
        @Valid @RequestBody CreateUserRequest request
    ) {
        log.info("POST /api/v1/users - Creating user: {}", request.getUsername());

        UserDTO createdUser = userService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * Update user.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD')")
    @Operation(
        summary = "Update user",
        description = "Update an existing user. Requires ADMIN or TECH_LEAD role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<UserDTO> updateUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable UUID id,
        @Parameter(description = "User update request", required = true)
        @Valid @RequestBody UpdateUserRequest request
    ) {
        log.info("PUT /api/v1/users/{}", id);

        UserDTO updatedUser = userService.updateUser(id, request);

        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete user.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete user",
        description = "Delete a user. Requires ADMIN role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable UUID id
    ) {
        log.info("DELETE /api/v1/users/{}", id);

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Activate user.
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD')")
    @Operation(
        summary = "Activate user",
        description = "Activate a deactivated user. Requires ADMIN or TECH_LEAD role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User activated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDTO> activateUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable UUID id
    ) {
        log.info("PATCH /api/v1/users/{}/activate", id);

        UserDTO user = userService.activateUser(id);

        return ResponseEntity.ok(user);
    }

    /**
     * Deactivate user.
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD')")
    @Operation(
        summary = "Deactivate user",
        description = "Deactivate an active user. Requires ADMIN or TECH_LEAD role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDTO> deactivateUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable UUID id
    ) {
        log.info("PATCH /api/v1/users/{}/deactivate", id);

        UserDTO user = userService.deactivateUser(id);

        return ResponseEntity.ok(user);
    }
}
