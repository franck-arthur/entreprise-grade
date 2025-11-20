package com.enterprise.app.application.service.audit;

import com.enterprise.app.application.dto.audit.AuditEventDTO;
import com.enterprise.app.application.dto.audit.AuditEventQuery;
import com.enterprise.app.application.dto.audit.AuditStatisticsDTO;
import com.enterprise.app.domain.model.AuditEventProjection;
import com.enterprise.app.domain.model.AuditEventType;
import com.enterprise.app.domain.port.AuditEventQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CQRS Query Service - Handles read operations for audit events.
 *
 * This service implements the QUERY side of CQRS:
 * - Reads from the projection model (read-optimized)
 * - Provides complex queries and aggregations
 * - Uses caching for frequently accessed data
 * - Never writes to the projection (that's done by command service)
 *
 * Key CQRS principles:
 * 1. Read operations use projection model (optimized indexes)
 * 2. Complex queries and aggregations without impacting writes
 * 3. Caching strategies for performance
 * 4. Denormalized data for faster queries
 *
 * Follows hexagonal architecture by depending on ports (interfaces)
 * instead of concrete implementations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditEventQueryPort auditEventQueryPort;

    /**
     * Find audit events by complex query (QUERY).
     *
     * Uses the read-optimized projection model with multiple indexes.
     */
    public Page<AuditEventDTO> findEvents(AuditEventQuery query, Pageable pageable) {
        log.debug("Querying audit events with filters");

        Page<AuditEventProjection> projections = auditEventQueryPort.findByFilters(
            query.getEventTypes(),
            query.getEventCategory(),
            query.getUserId(),
            query.getUsername(),
            query.getTargetEntityType(),
            query.getTargetEntityId(),
            query.getSuccess(),
            query.getFromDate(),
            query.getToDate(),
            pageable
        );

        return projections.map(this::toDTO);
    }

    /**
     * Find all audit events with pagination.
     */
    public Page<AuditEventDTO> findAllEvents(Pageable pageable) {
        log.debug("Finding all audit events - page: {}, size: {}",
            pageable.getPageNumber(), pageable.getPageSize());

        return auditEventQueryPort.findAll(pageable)
            .map(this::toDTO);
    }

    /**
     * Find events by user.
     */
    public Page<AuditEventDTO> findEventsByUser(UUID userId, Pageable pageable) {
        log.debug("Finding audit events for user: {}", userId);

        return auditEventQueryPort.findByUserIdOrderByTimestampDesc(userId, pageable)
            .map(this::toDTO);
    }

    /**
     * Find events by type.
     */
    public Page<AuditEventDTO> findEventsByType(AuditEventType eventType, Pageable pageable) {
        log.debug("Finding audit events by type: {}", eventType);

        return auditEventQueryPort.findByEventTypeOrderByTimestampDesc(eventType, pageable)
            .map(this::toDTO);
    }

    /**
     * Find events by category.
     */
    public Page<AuditEventDTO> findEventsByCategory(String category, Pageable pageable) {
        log.debug("Finding audit events by category: {}", category);

        return auditEventQueryPort.findByEventCategoryOrderByTimestampDesc(category, pageable)
            .map(this::toDTO);
    }

    /**
     * Find events by success status.
     */
    public Page<AuditEventDTO> findEventsBySuccess(boolean success, Pageable pageable) {
        log.debug("Finding audit events by success: {}", success);

        return auditEventQueryPort.findBySuccessOrderByTimestampDesc(success, pageable)
            .map(this::toDTO);
    }

    /**
     * Find events by date range.
     */
    public Page<AuditEventDTO> findEventsByDateRange(
        LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable
    ) {
        log.debug("Finding audit events between {} and {}", fromDate, toDate);

        return auditEventQueryPort.findByTimestampBetweenOrderByTimestampDesc(
                fromDate, toDate, pageable)
            .map(this::toDTO);
    }

    /**
     * Find events for target entity.
     */
    public Page<AuditEventDTO> findEventsByTargetEntity(
        String entityType, UUID entityId, Pageable pageable
    ) {
        log.debug("Finding audit events for entity: {} with id: {}", entityType, entityId);

        return auditEventQueryPort.findByTargetEntityTypeAndTargetEntityIdOrderByTimestampDesc(
                entityType, entityId, pageable)
            .map(this::toDTO);
    }

    /**
     * Get audit statistics (QUERY with aggregations).
     *
     * Demonstrates CQRS benefit: complex aggregations without impacting writes.
     * Uses cached results for better performance.
     */
    @Cacheable(value = "auditStatistics", key = "'all'")
    public AuditStatisticsDTO getStatistics() {
        log.debug("Calculating audit statistics");

        AuditStatisticsDTO.AuditStatisticsDTOBuilder stats = AuditStatisticsDTO.builder();

        // Basic counts
        stats.totalEvents(auditEventQueryPort.count());
        stats.successfulEvents(auditEventQueryPort.countBySuccess(true));
        stats.failedEvents(auditEventQueryPort.countBySuccess(false));

        // Events by type
        Map<String, Long> eventsByType = new HashMap<>();
        auditEventQueryPort.countByEventTypeGrouped().forEach(result -> {
            eventsByType.put(result[0].toString(), (Long) result[1]);
        });
        stats.eventsByType(eventsByType);

        // Events by category
        Map<String, Long> eventsByCategory = new HashMap<>();
        auditEventQueryPort.countByCategoryGrouped().forEach(result -> {
            eventsByCategory.put((String) result[0], (Long) result[1]);
        });
        stats.eventsByCategory(eventsByCategory);

        // Top users
        Map<String, Long> topUsers = new HashMap<>();
        auditEventQueryPort.findTopUsersByEventCount(PageRequest.of(0, 10))
            .forEach(result -> {
                topUsers.put((String) result[0], (Long) result[1]);
            });
        stats.topUsers(topUsers);

        // Top target entities
        Map<String, Long> topEntities = new HashMap<>();
        auditEventQueryPort.findTopTargetEntitiesByEventCount(PageRequest.of(0, 10))
            .forEach(result -> {
                topEntities.put((String) result[0], (Long) result[1]);
            });
        stats.topTargetEntities(topEntities);

        return stats.build();
    }

    /**
     * Get statistics for date range.
     */
    @Cacheable(value = "auditStatistics", key = "#fromDate + '-' + #toDate")
    public AuditStatisticsDTO getStatisticsForDateRange(
        LocalDateTime fromDate, LocalDateTime toDate
    ) {
        log.debug("Calculating audit statistics for date range: {} to {}", fromDate, toDate);

        AuditStatisticsDTO.AuditStatisticsDTOBuilder stats = AuditStatisticsDTO.builder();

        // Count in date range
        long totalInRange = auditEventQueryPort.countByTimestampBetween(fromDate, toDate);
        stats.totalEvents(totalInRange);

        // Events by date
        Map<String, Long> eventsByDate = new HashMap<>();
        auditEventQueryPort.countByDateRange(fromDate, toDate).forEach(result -> {
            eventsByDate.put(result[0].toString(), (Long) result[1]);
        });
        stats.eventsByDate(eventsByDate);

        return stats.build();
    }

    /**
     * Get hourly statistics for a specific date.
     */
    public Map<String, Long> getHourlyStatistics(LocalDate date) {
        log.debug("Calculating hourly statistics for date: {}", date);

        Map<String, Long> hourlyStats = new HashMap<>();
        Date sqlDate = Date.valueOf(date);

        auditEventQueryPort.countByHourForDate(sqlDate).forEach(result -> {
            Integer hour = (Integer) result[0];
            Long count = (Long) result[1];
            hourlyStats.put(hour.toString(), count);
        });

        return hourlyStats;
    }

    /**
     * Convert projection to DTO.
     */
    private AuditEventDTO toDTO(AuditEventProjection projection) {
        return AuditEventDTO.builder()
            .id(projection.getId())
            .eventType(projection.getEventType())
            .eventCategory(projection.getEventCategory())
            .userId(projection.getUserId())
            .username(projection.getUsername())
            .targetEntityType(projection.getTargetEntityType())
            .targetEntityId(projection.getTargetEntityId())
            .targetEntityName(projection.getTargetEntityName())
            .ipAddress(projection.getIpAddress())
            .userAgent(projection.getUserAgent())
            .details(projection.getDetails())
            .success(projection.isSuccess())
            .errorMessage(projection.getErrorMessage())
            .timestamp(projection.getTimestamp())
            .build();
    }
}
