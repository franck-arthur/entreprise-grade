package com.enterprise.app.infrastructure.persistence.repository;

import com.enterprise.app.domain.model.AuditEventProjection;
import com.enterprise.app.domain.model.AuditEventType;
import com.enterprise.app.domain.port.AuditEventQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CQRS Query Repository - Read side.
 * Optimized for complex queries, filtering, and aggregations.
 * Uses indexes defined in AuditEventProjection for fast reads.
 *
 * Implements AuditEventQueryPort following hexagonal architecture.
 */
@Repository
public interface AuditEventProjectionRepository extends JpaRepository<AuditEventProjection, UUID>, AuditEventQueryPort {

    /**
     * Find events by user with pagination.
     */
    Page<AuditEventProjection> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Find events by event type.
     */
    Page<AuditEventProjection> findByEventTypeOrderByTimestampDesc(
        AuditEventType eventType, Pageable pageable
    );

    /**
     * Find events by category.
     */
    Page<AuditEventProjection> findByEventCategoryOrderByTimestampDesc(
        String category, Pageable pageable
    );

    /**
     * Find events by success status.
     */
    Page<AuditEventProjection> findBySuccessOrderByTimestampDesc(
        boolean success, Pageable pageable
    );

    /**
     * Find events by date range.
     */
    Page<AuditEventProjection> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable
    );

    /**
     * Find events by target entity.
     */
    Page<AuditEventProjection> findByTargetEntityTypeAndTargetEntityIdOrderByTimestampDesc(
        String entityType, UUID entityId, Pageable pageable
    );

    /**
     * Complex query with multiple filters.
     */
    @Query("SELECT a FROM AuditEventProjection a WHERE " +
           "(:eventTypes IS NULL OR a.eventType IN :eventTypes) AND " +
           "(:eventCategory IS NULL OR a.eventCategory = :eventCategory) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:username IS NULL OR a.username LIKE %:username%) AND " +
           "(:targetEntityType IS NULL OR a.targetEntityType = :targetEntityType) AND " +
           "(:targetEntityId IS NULL OR a.targetEntityId = :targetEntityId) AND " +
           "(:success IS NULL OR a.success = :success) AND " +
           "(:fromDate IS NULL OR a.timestamp >= :fromDate) AND " +
           "(:toDate IS NULL OR a.timestamp <= :toDate) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditEventProjection> findByFilters(
        @Param("eventTypes") List<AuditEventType> eventTypes,
        @Param("eventCategory") String eventCategory,
        @Param("userId") UUID userId,
        @Param("username") String username,
        @Param("targetEntityType") String targetEntityType,
        @Param("targetEntityId") UUID targetEntityId,
        @Param("success") Boolean success,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );

    /**
     * Count total events.
     */
    long count();

    /**
     * Count successful events.
     */
    long countBySuccess(boolean success);

    /**
     * Count events by type.
     */
    long countByEventType(AuditEventType eventType);

    /**
     * Count events by category.
     */
    long countByEventCategory(String category);

    /**
     * Count events by date range.
     */
    long countByTimestampBetween(LocalDateTime fromDate, LocalDateTime toDate);

    /**
     * Get events count grouped by type.
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditEventProjection a " +
           "GROUP BY a.eventType ORDER BY COUNT(a) DESC")
    List<Object[]> countByEventTypeGrouped();

    /**
     * Get events count grouped by category.
     */
    @Query("SELECT a.eventCategory, COUNT(a) FROM AuditEventProjection a " +
           "GROUP BY a.eventCategory ORDER BY COUNT(a) DESC")
    List<Object[]> countByCategoryGrouped();

    /**
     * Get events count grouped by hour.
     */
    @Query("SELECT a.eventHour, COUNT(a) FROM AuditEventProjection a " +
           "WHERE a.eventDate = :date " +
           "GROUP BY a.eventHour ORDER BY a.eventHour")
    List<Object[]> countByHourForDate(@Param("date") Date date);

    /**
     * Get events count grouped by date.
     */
    @Query("SELECT a.eventDate, COUNT(a) FROM AuditEventProjection a " +
           "WHERE a.timestamp BETWEEN :fromDate AND :toDate " +
           "GROUP BY a.eventDate ORDER BY a.eventDate")
    List<Object[]> countByDateRange(
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate
    );

    /**
     * Get top users by event count.
     */
    @Query("SELECT a.username, COUNT(a) FROM AuditEventProjection a " +
           "WHERE a.username IS NOT NULL " +
           "GROUP BY a.username ORDER BY COUNT(a) DESC")
    List<Object[]> findTopUsersByEventCount(Pageable pageable);

    /**
     * Get top target entities by event count.
     */
    @Query("SELECT a.targetEntityType, COUNT(a) FROM AuditEventProjection a " +
           "WHERE a.targetEntityType IS NOT NULL " +
           "GROUP BY a.targetEntityType ORDER BY COUNT(a) DESC")
    List<Object[]> findTopTargetEntitiesByEventCount(Pageable pageable);
}
