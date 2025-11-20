package com.enterprise.app.presentation.controller;

import com.enterprise.app.application.dto.audit.AuditEventDTO;
import com.enterprise.app.application.dto.audit.AuditEventQuery;
import com.enterprise.app.application.dto.audit.AuditStatisticsDTO;
import com.enterprise.app.application.service.audit.AuditQueryService;
import com.enterprise.app.domain.model.AuditEventType;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Audit Events.
 *
 * Implements CQRS QUERY side - all endpoints are read-only.
 * Command operations (write) are handled internally by AuditCommandService.
 *
 * This demonstrates CQRS separation:
 * - No POST/PUT/DELETE endpoints (commands handled internally)
 * - Only GET endpoints for queries
 * - Uses read-optimized projections
 * - Supports complex filtering and aggregations
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit", description = "Audit events and logs API (CQRS Query Side)")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditQueryService auditQueryService;

    /**
     * Get all audit events with optional filters (CQRS QUERY).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get all audit events with filters",
        description = "Query audit events with complex filters. " +
            "Demonstrates CQRS query side with read-optimized projections."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved audit events"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<AuditEventDTO>> getAuditEvents(
        @Parameter(description = "Event types filter")
        @RequestParam(required = false) List<AuditEventType> eventTypes,

        @Parameter(description = "Event category filter (USER, AUTH, BATCH, SECURITY, SYSTEM)")
        @RequestParam(required = false) String eventCategory,

        @Parameter(description = "User ID filter")
        @RequestParam(required = false) UUID userId,

        @Parameter(description = "Username filter (partial match)")
        @RequestParam(required = false) String username,

        @Parameter(description = "Target entity type filter")
        @RequestParam(required = false) String targetEntityType,

        @Parameter(description = "Target entity ID filter")
        @RequestParam(required = false) UUID targetEntityId,

        @Parameter(description = "Success filter")
        @RequestParam(required = false) Boolean success,

        @Parameter(description = "From date filter")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime fromDate,

        @Parameter(description = "To date filter")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime toDate,

        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 50, sort = "timestamp") Pageable pageable
    ) {
        log.debug("GET /api/v1/audit - filters: eventTypes={}, category={}, userId={}",
            eventTypes, eventCategory, userId);

        AuditEventQuery query = AuditEventQuery.builder()
            .eventTypes(eventTypes)
            .eventCategory(eventCategory)
            .userId(userId)
            .username(username)
            .targetEntityType(targetEntityType)
            .targetEntityId(targetEntityId)
            .success(success)
            .fromDate(fromDate)
            .toDate(toDate)
            .build();

        Page<AuditEventDTO> events = auditQueryService.findEvents(query, pageable);

        return ResponseEntity.ok(events);
    }

    /**
     * Get audit events by user.
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get audit events for specific user",
        description = "Query all audit events for a specific user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user events"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<AuditEventDTO>> getAuditEventsByUser(
        @Parameter(description = "User ID", required = true)
        @PathVariable UUID userId,

        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 50, sort = "timestamp") Pageable pageable
    ) {
        log.debug("GET /api/v1/audit/user/{}", userId);

        Page<AuditEventDTO> events = auditQueryService.findEventsByUser(userId, pageable);

        return ResponseEntity.ok(events);
    }

    /**
     * Get audit events by type.
     */
    @GetMapping("/type/{eventType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get audit events by type",
        description = "Query all audit events of a specific type"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved events by type"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<AuditEventDTO>> getAuditEventsByType(
        @Parameter(description = "Event type", required = true)
        @PathVariable AuditEventType eventType,

        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 50, sort = "timestamp") Pageable pageable
    ) {
        log.debug("GET /api/v1/audit/type/{}", eventType);

        Page<AuditEventDTO> events = auditQueryService.findEventsByType(eventType, pageable);

        return ResponseEntity.ok(events);
    }

    /**
     * Get audit events for target entity.
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get audit events for target entity",
        description = "Query all audit events for a specific entity (e.g., User, BatchImport)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved entity events"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<AuditEventDTO>> getAuditEventsByEntity(
        @Parameter(description = "Entity type", required = true)
        @PathVariable String entityType,

        @Parameter(description = "Entity ID", required = true)
        @PathVariable UUID entityId,

        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 50, sort = "timestamp") Pageable pageable
    ) {
        log.debug("GET /api/v1/audit/entity/{}/{}", entityType, entityId);

        Page<AuditEventDTO> events = auditQueryService.findEventsByTargetEntity(
            entityType, entityId, pageable);

        return ResponseEntity.ok(events);
    }

    /**
     * Get audit statistics (CQRS QUERY with aggregations).
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get audit statistics",
        description = "Get aggregated statistics for audit events. " +
            "Demonstrates CQRS benefit: complex aggregations without impacting writes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<AuditStatisticsDTO> getStatistics() {
        log.debug("GET /api/v1/audit/statistics");

        AuditStatisticsDTO statistics = auditQueryService.getStatistics();

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get audit statistics for date range.
     */
    @GetMapping("/statistics/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get audit statistics for date range",
        description = "Get aggregated statistics for a specific date range"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<AuditStatisticsDTO> getStatisticsForDateRange(
        @Parameter(description = "From date", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,

        @Parameter(description = "To date", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        log.debug("GET /api/v1/audit/statistics/range - from: {}, to: {}", fromDate, toDate);

        AuditStatisticsDTO statistics = auditQueryService.getStatisticsForDateRange(
            fromDate, toDate);

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get hourly statistics for a specific date.
     */
    @GetMapping("/statistics/hourly")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "Get hourly statistics",
        description = "Get event counts grouped by hour for a specific date"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved hourly stats"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, Long>> getHourlyStatistics(
        @Parameter(description = "Date", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.debug("GET /api/v1/audit/statistics/hourly - date: {}", date);

        Map<String, Long> hourlyStats = auditQueryService.getHourlyStatistics(date);

        return ResponseEntity.ok(hourlyStats);
    }
}
