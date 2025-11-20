package com.enterprise.app.domain.port;

import com.enterprise.app.domain.model.AuditEventProjection;
import com.enterprise.app.domain.model.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for audit event query operations (CQRS READ side).
 * Follows hexagonal architecture pattern.
 *
 * This port defines the contract for read operations on audit events
 * without coupling the domain to any specific implementation.
 * Optimized for complex queries, filtering, and aggregations.
 */
public interface AuditEventQueryPort {

    /**
     * Save an audit event projection (for eventual consistency).
     *
     * @param projection The audit event projection to save
     * @return The saved projection
     */
    AuditEventProjection save(AuditEventProjection projection);

    /**
     * Find projection by ID.
     *
     * @param id The projection ID
     * @return The projection if found
     */
    Optional<AuditEventProjection> findById(UUID id);

    /**
     * Find all projections with pagination.
     *
     * @param pageable Pagination parameters
     * @return Page of projections
     */
    Page<AuditEventProjection> findAll(Pageable pageable);

    /**
     * Find events by user with pagination.
     *
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of projections
     */
    Page<AuditEventProjection> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Find events by event type.
     *
     * @param eventType Event type
     * @param pageable Pagination parameters
     * @return Page of projections
     */
    Page<AuditEventProjection> findByEventTypeOrderByTimestampDesc(
        AuditEventType eventType, Pageable pageable
    );

    /**
     * Find events by category.
     *
     * @param category Event category
     * @param pageable Pagination parameters
     * @return Page of projections
     */
    Page<AuditEventProjection> findByEventCategoryOrderByTimestampDesc(
        String category, Pageable pageable
    );

    /**
     * Find events by success status.
     *
     * @param success Success status
     * @param pageable Pagination parameters
     * @return Page of projections
     */
    Page<AuditEventProjection> findBySuccessOrderByTimestampDesc(
        boolean success, Pageable pageable
    );

    /**
     * Find events by date range.
     *
     * @param fromDate Start date
     * @param toDate End date
     * @param pageable Pagination parameters
     * @return Page of projections
     */
    Page<AuditEventProjection> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable
    );

    /**
     * Find events by target entity.
     *
     * @param entityType Entity type
     * @param entityId Entity ID
     * @param pageable Pagination parameters
     * @return Page of projections
     */
    Page<AuditEventProjection> findByTargetEntityTypeAndTargetEntityIdOrderByTimestampDesc(
        String entityType, UUID entityId, Pageable pageable
    );

    /**
     * Complex query with multiple filters.
     *
     * @param eventTypes Event types filter
     * @param eventCategory Event category filter
     * @param userId User ID filter
     * @param username Username filter (partial match)
     * @param targetEntityType Target entity type filter
     * @param targetEntityId Target entity ID filter
     * @param success Success status filter
     * @param fromDate From date filter
     * @param toDate To date filter
     * @param pageable Pagination parameters
     * @return Page of projections
     */
    Page<AuditEventProjection> findByFilters(
        List<AuditEventType> eventTypes,
        String eventCategory,
        UUID userId,
        String username,
        String targetEntityType,
        UUID targetEntityId,
        Boolean success,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        Pageable pageable
    );

    /**
     * Count total events.
     *
     * @return Total count
     */
    long count();

    /**
     * Count successful or failed events.
     *
     * @param success Success status
     * @return Count
     */
    long countBySuccess(boolean success);

    /**
     * Count events by type.
     *
     * @param eventType Event type
     * @return Count
     */
    long countByEventType(AuditEventType eventType);

    /**
     * Count events by category.
     *
     * @param category Event category
     * @return Count
     */
    long countByEventCategory(String category);

    /**
     * Count events by date range.
     *
     * @param fromDate Start date
     * @param toDate End date
     * @return Count
     */
    long countByTimestampBetween(LocalDateTime fromDate, LocalDateTime toDate);

    /**
     * Get events count grouped by type.
     *
     * @return List of [eventType, count] pairs
     */
    List<Object[]> countByEventTypeGrouped();

    /**
     * Get events count grouped by category.
     *
     * @return List of [category, count] pairs
     */
    List<Object[]> countByCategoryGrouped();

    /**
     * Get events count grouped by hour.
     *
     * @param date The date to query
     * @return List of [hour, count] pairs
     */
    List<Object[]> countByHourForDate(Date date);

    /**
     * Get events count grouped by date.
     *
     * @param fromDate Start date
     * @param toDate End date
     * @return List of [date, count] pairs
     */
    List<Object[]> countByDateRange(LocalDateTime fromDate, LocalDateTime toDate);

    /**
     * Get top users by event count.
     *
     * @param pageable Pagination parameters
     * @return List of [username, count] pairs
     */
    List<Object[]> findTopUsersByEventCount(Pageable pageable);

    /**
     * Get top target entities by event count.
     *
     * @param pageable Pagination parameters
     * @return List of [entityType, count] pairs
     */
    List<Object[]> findTopTargetEntitiesByEventCount(Pageable pageable);

    /**
     * Delete all audit event projections.
     * Use with caution - typically for data migration or rebuild operations.
     */
    void deleteAll();
}
