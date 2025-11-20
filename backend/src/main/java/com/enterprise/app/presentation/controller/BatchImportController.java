package com.enterprise.app.presentation.controller;

import com.enterprise.app.application.dto.BatchImportDetailResponse;
import com.enterprise.app.application.dto.BatchImportResponse;
import com.enterprise.app.application.mapper.BatchImportMapper;
import com.enterprise.app.application.service.BatchImportService;
import com.enterprise.app.application.usecase.UserService;
import com.enterprise.app.domain.model.BatchImport;
import com.enterprise.app.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST Controller for Batch Import operations.
 *
 * Provides endpoints for uploading CSV files and tracking import progress.
 * Uses multithreading for efficient processing of large files.
 */
@RestController
@RequestMapping("/api/v1/batch-imports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Batch Imports", description = "Batch import management API")
@SecurityRequirement(name = "bearerAuth")
public class BatchImportController {

    private final BatchImportService batchImportService;
    private final UserService userService;
    private final BatchImportMapper batchImportMapper;

    /**
     * Upload a CSV file for batch import.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Upload CSV file for batch import",
        description = "Upload a CSV file to import users in batch. " +
            "Expected CSV format: username,email,firstName,lastName,phoneNumber,roles. " +
            "Processing is done asynchronously with multithreading."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Import created and processing started"),
        @ApiResponse(responseCode = "400", description = "Invalid file or format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<BatchImportResponse> uploadCsvFile(
        @Parameter(description = "CSV file to import", required = true)
        @RequestParam("file") MultipartFile file,
        Authentication authentication
    ) {
        log.info("POST /api/v1/batch-imports/upload - File: {}", file.getOriginalFilename());

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (!file.getOriginalFilename().endsWith(".csv")) {
            throw new IllegalArgumentException("File must be a CSV file");
        }

        // Get current user
        User currentUser = getCurrentUser(authentication);

        // Create batch import
        BatchImport batchImport = batchImportService.createBatchImport(file, currentUser);
        BatchImportResponse response = batchImportMapper.toResponse(batchImport);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get batch import by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get batch import by ID",
        description = "Retrieve details of a specific batch import including all processed lines."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved batch import"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Batch import not found")
    })
    public ResponseEntity<BatchImportDetailResponse> getBatchImportById(
        @Parameter(description = "Batch import ID", required = true)
        @PathVariable UUID id
    ) {
        log.debug("GET /api/v1/batch-imports/{}", id);

        BatchImport batchImport = batchImportService.getBatchImportById(id);
        BatchImportDetailResponse response = batchImportMapper.toDetailResponse(batchImport);

        return ResponseEntity.ok(response);
    }

    /**
     * Get batch import status (lightweight endpoint for polling).
     */
    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get batch import status",
        description = "Retrieve current status and progress of a batch import. " +
            "Lightweight endpoint suitable for polling."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved status"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Batch import not found")
    })
    public ResponseEntity<BatchImportResponse> getBatchImportStatus(
        @Parameter(description = "Batch import ID", required = true)
        @PathVariable UUID id
    ) {
        log.debug("GET /api/v1/batch-imports/{}/status", id);

        BatchImport batchImport = batchImportService.getBatchImportById(id);
        BatchImportResponse response = batchImportMapper.toResponse(batchImport);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all batch imports with pagination.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get all batch imports",
        description = "Retrieve a paginated list of all batch imports."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved batch imports"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<BatchImportResponse>> getAllBatchImports(
        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        log.debug("GET /api/v1/batch-imports - page: {}, size: {}",
            pageable.getPageNumber(), pageable.getPageSize());

        Page<BatchImport> batchImports = batchImportService.getAllBatchImports(pageable);
        Page<BatchImportResponse> response = batchImports.map(batchImportMapper::toResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Get batch imports for current user.
     */
    @GetMapping("/my-imports")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get my batch imports",
        description = "Retrieve batch imports initiated by the current user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved batch imports"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<BatchImportResponse>> getMyBatchImports(
        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
        Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        log.debug("GET /api/v1/batch-imports/my-imports - User: {}", currentUser.getUsername());

        Page<BatchImport> batchImports = batchImportService.getBatchImportsByUser(
            currentUser.getId(), pageable);
        Page<BatchImportResponse> response = batchImports.map(batchImportMapper::toResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a batch import.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD')")
    @Operation(
        summary = "Cancel batch import",
        description = "Cancel a batch import that is currently in progress. " +
            "Requires ADMIN or TECH_LEAD role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Batch import cancelled"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Batch import not found")
    })
    public ResponseEntity<BatchImportResponse> cancelBatchImport(
        @Parameter(description = "Batch import ID", required = true)
        @PathVariable UUID id
    ) {
        log.info("POST /api/v1/batch-imports/{}/cancel", id);

        batchImportService.cancelBatchImport(id);
        BatchImport batchImport = batchImportService.getBatchImportById(id);
        BatchImportResponse response = batchImportMapper.toResponse(batchImport);

        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user.
     */
    private User getCurrentUser(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String username = jwt.getClaimAsString("preferred_username");

        if (username == null) {
            username = jwt.getSubject();
        }

        return userService.getUserEntityByUsername(username);
    }
}
