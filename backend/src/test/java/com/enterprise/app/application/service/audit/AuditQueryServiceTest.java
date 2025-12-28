package com.enterprise.app.application.service.audit;

import com.enterprise.app.application.dto.audit.AuditEventDTO;
import com.enterprise.app.application.dto.audit.AuditEventQuery;
import com.enterprise.app.application.dto.audit.AuditStatisticsDTO;
import com.enterprise.app.domain.model.AuditEventProjection;
import com.enterprise.app.domain.model.AuditEventType;
import com.enterprise.app.domain.port.AuditEventQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditQueryService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditQueryService Tests")
class AuditQueryServiceTest {

    @Mock
    private AuditEventQueryPort auditEventQueryPort;

    @InjectMocks
    private AuditQueryService auditQueryService;

    private AuditEventProjection testProjection;
    private Long testUserId;
    private Long testTargetEntityId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testTargetEntityId = 2L;
        pageable = PageRequest.of(0, 10);

        testProjection = AuditEventProjection.builder()
            .id(1L)
            .eventType(AuditEventType.USER_CREATED)
            .eventCategory("USER")
            .userId(1L)
            .username("testuser")
            .targetEntityType("User")
            .targetEntityId("1")
            .targetEntityName("John Doe")
            .ipAddress("192.168.1.100")
            .userAgent("Mozilla/5.0")
            .details("User created successfully")
            .success(true)
            .timestamp(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("Should find all events with pagination")
    void shouldFindAllEvents() {
        // Given
        List<AuditEventProjection> projections = Arrays.asList(testProjection);
        Page<AuditEventProjection> page = new PageImpl<>(projections, pageable, 1);

        when(auditEventQueryPort.findAll(pageable)).thenReturn(page);

        // When
        Page<AuditEventDTO> result = auditQueryService.findAllEvents(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(testProjection.getId());
        assertThat(result.getContent().get(0).getEventType()).isEqualTo(AuditEventType.USER_CREATED);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser");

        verify(auditEventQueryPort).findAll(pageable);
    }
/*
    @Test
    @DisplayName("Should find events by complex query")
    void shouldFindEventsByQuery() {
        // Given
        AuditEventQuery query = AuditEventQuery.builder()
            .eventTypes(Arrays.asList(AuditEventType.USER_CREATED, AuditEventType.USER_UPDATED))
            .eventCategory("USER")
            .userId(testUserId)
            .username("testuser")
            .success(true)
            .build();

        List<AuditEventProjection> projections = Arrays.asList(testProjection);
        Page<AuditEventProjection> page = new PageImpl<>(projections, pageable, 1);

        when(auditEventQueryPort.findByFilters(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(page);

        // When
        Page<AuditEventDTO> result = auditQueryService.findEvents(query, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEventCategory()).isEqualTo("USER");

        verify(auditEventQueryPort).findByFilters(
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
    }
*/
    @Test
    @DisplayName("Should find events by user")
    void shouldFindEventsByUser() {
        // Given
        List<AuditEventProjection> projections = Arrays.asList(testProjection);
        Page<AuditEventProjection> page = new PageImpl<>(projections, pageable, 1);

        when(auditEventQueryPort.findByUserIdOrderByTimestampDesc(testUserId, pageable))
            .thenReturn(page);

        // When
        Page<AuditEventDTO> result = auditQueryService.findEventsByUser(testUserId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isNotNull();

        verify(auditEventQueryPort).findByUserIdOrderByTimestampDesc(testUserId, pageable);
    }

    @Test
    @DisplayName("Should find events by type")
    void shouldFindEventsByType() {
        // Given
        List<AuditEventProjection> projections = Arrays.asList(testProjection);
        Page<AuditEventProjection> page = new PageImpl<>(projections, pageable, 1);

        when(auditEventQueryPort.findByEventTypeOrderByTimestampDesc(
            AuditEventType.USER_CREATED, pageable
        )).thenReturn(page);

        // When
        Page<AuditEventDTO> result = auditQueryService.findEventsByType(
            AuditEventType.USER_CREATED, pageable
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEventType()).isEqualTo(AuditEventType.USER_CREATED);

        verify(auditEventQueryPort).findByEventTypeOrderByTimestampDesc(
            AuditEventType.USER_CREATED, pageable
        );
    }

    @Test
    @DisplayName("Should find events by category")
    void shouldFindEventsByCategory() {
        // Given
        List<AuditEventProjection> projections = Arrays.asList(testProjection);
        Page<AuditEventProjection> page = new PageImpl<>(projections, pageable, 1);

        when(auditEventQueryPort.findByEventCategoryOrderByTimestampDesc("USER", pageable))
            .thenReturn(page);

        // When
        Page<AuditEventDTO> result = auditQueryService.findEventsByCategory("USER", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEventCategory()).isEqualTo("USER");

        verify(auditEventQueryPort).findByEventCategoryOrderByTimestampDesc("USER", pageable);
    }

    @Test
    @DisplayName("Should find events by success status")
    void shouldFindEventsBySuccess() {
        // Given
        List<AuditEventProjection> projections = Arrays.asList(testProjection);
        Page<AuditEventProjection> page = new PageImpl<>(projections, pageable, 1);

        when(auditEventQueryPort.findBySuccessOrderByTimestampDesc(true, pageable))
            .thenReturn(page);

        // When
        Page<AuditEventDTO> result = auditQueryService.findEventsBySuccess(true, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isSuccess()).isTrue();

        verify(auditEventQueryPort).findBySuccessOrderByTimestampDesc(true, pageable);
    }

    @Test
    @DisplayName("Should find events by date range")
    void shouldFindEventsByDateRange() {
        // Given
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();

        List<AuditEventProjection> projections = Arrays.asList(testProjection);
        Page<AuditEventProjection> page = new PageImpl<>(projections, pageable, 1);

        when(auditEventQueryPort.findByTimestampBetweenOrderByTimestampDesc(
            fromDate, toDate, pageable
        )).thenReturn(page);

        // When
        Page<AuditEventDTO> result = auditQueryService.findEventsByDateRange(
            fromDate, toDate, pageable
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(auditEventQueryPort).findByTimestampBetweenOrderByTimestampDesc(
            fromDate, toDate, pageable
        );
    }

    @Test
    @DisplayName("Should find events by target entity")
    void shouldFindEventsByTargetEntity() {
        // Given
        List<AuditEventProjection> projections = Arrays.asList(testProjection);
        Page<AuditEventProjection> page = new PageImpl<>(projections, pageable, 1);

        when(auditEventQueryPort.findByTargetEntityTypeAndTargetEntityIdOrderByTimestampDesc(
            "User", testTargetEntityId, pageable
        )).thenReturn(page);

        // When
        Page<AuditEventDTO> result = auditQueryService.findEventsByTargetEntity(
            "User", testTargetEntityId, pageable
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTargetEntityType()).isEqualTo("User");
        assertThat(result.getContent().get(0).getTargetEntityId()).isNotNull();

        verify(auditEventQueryPort).findByTargetEntityTypeAndTargetEntityIdOrderByTimestampDesc(
            "User", testTargetEntityId, pageable
        );
    }

    @Test
    @DisplayName("Should get audit statistics")
    void shouldGetStatistics() {
        // Given
        when(auditEventQueryPort.count()).thenReturn(100L);
        when(auditEventQueryPort.countBySuccess(true)).thenReturn(80L);
        when(auditEventQueryPort.countBySuccess(false)).thenReturn(20L);

        List<Object[]> eventsByType = Arrays.asList(
            new Object[]{AuditEventType.USER_CREATED, 50L},
            new Object[]{AuditEventType.USER_UPDATED, 30L},
            new Object[]{AuditEventType.LOGIN_SUCCESS, 20L}
        );
        when(auditEventQueryPort.countByEventTypeGrouped()).thenReturn(eventsByType);

        List<Object[]> eventsByCategory = Arrays.asList(
            new Object[]{"USER", 80L},
            new Object[]{"AUTH", 20L}
        );
        when(auditEventQueryPort.countByCategoryGrouped()).thenReturn(eventsByCategory);

        List<Object[]> topUsers = Arrays.asList(
            new Object[]{"user1", 50L},
            new Object[]{"user2", 30L}
        );
        when(auditEventQueryPort.findTopUsersByEventCount(any(Pageable.class)))
            .thenReturn(topUsers);

        List<Object[]> topEntities = Arrays.asList(
            new Object[]{"User", 60L},
            new Object[]{"Role", 40L}
        );
        when(auditEventQueryPort.findTopTargetEntitiesByEventCount(any(Pageable.class)))
            .thenReturn(topEntities);

        // When
        AuditStatisticsDTO result = auditQueryService.getStatistics();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalEvents()).isEqualTo(100L);
        assertThat(result.getSuccessfulEvents()).isEqualTo(80L);
        assertThat(result.getFailedEvents()).isEqualTo(20L);
        assertThat(result.getEventsByType()).hasSize(3);
        assertThat(result.getEventsByCategory()).hasSize(2);
        assertThat(result.getTopUsers()).hasSize(2);
        assertThat(result.getTopTargetEntities()).hasSize(2);

        verify(auditEventQueryPort).count();
        verify(auditEventQueryPort).countBySuccess(true);
        verify(auditEventQueryPort).countBySuccess(false);
        verify(auditEventQueryPort).countByEventTypeGrouped();
        verify(auditEventQueryPort).countByCategoryGrouped();
        verify(auditEventQueryPort).findTopUsersByEventCount(any(Pageable.class));
        verify(auditEventQueryPort).findTopTargetEntitiesByEventCount(any(Pageable.class));
    }

    @Test
    @DisplayName("Should get statistics for date range")
    void shouldGetStatisticsForDateRange() {
        // Given
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();

        when(auditEventQueryPort.countByTimestampBetween(fromDate, toDate)).thenReturn(50L);

        List<Object[]> eventsByDate = Arrays.asList(
            new Object[]{Date.valueOf(LocalDate.now()), 20L},
            new Object[]{Date.valueOf(LocalDate.now().minusDays(1)), 15L},
            new Object[]{Date.valueOf(LocalDate.now().minusDays(2)), 15L}
        );
        when(auditEventQueryPort.countByDateRange(fromDate, toDate))
            .thenReturn(eventsByDate);

        // When
        AuditStatisticsDTO result = auditQueryService.getStatisticsForDateRange(fromDate, toDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalEvents()).isEqualTo(50L);
        assertThat(result.getEventsByDate()).hasSize(3);

        verify(auditEventQueryPort).countByTimestampBetween(fromDate, toDate);
        verify(auditEventQueryPort).countByDateRange(fromDate, toDate);
    }

    @Test
    @DisplayName("Should get hourly statistics")
    void shouldGetHourlyStatistics() {
        // Given
        LocalDate date = LocalDate.now();
        Date sqlDate = Date.valueOf(date);

        List<Object[]> hourlyData = Arrays.asList(
            new Object[]{9, 10L},
            new Object[]{10, 15L},
            new Object[]{11, 20L},
            new Object[]{14, 25L}
        );
        when(auditEventQueryPort.countByHourForDate(sqlDate)).thenReturn(hourlyData);

        // When
        Map<String, Long> result = auditQueryService.getHourlyStatistics(date);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);
        assertThat(result.get("9")).isEqualTo(10L);
        assertThat(result.get("10")).isEqualTo(15L);
        assertThat(result.get("11")).isEqualTo(20L);
        assertThat(result.get("14")).isEqualTo(25L);

        verify(auditEventQueryPort).countByHourForDate(sqlDate);
    }

    @Test
    @DisplayName("Should return empty page when no events found")
    void shouldReturnEmptyPageWhenNoEventsFound() {
        // Given
        Page<AuditEventProjection> emptyPage = Page.empty(pageable);
        when(auditEventQueryPort.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<AuditEventDTO> result = auditQueryService.findAllEvents(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(auditEventQueryPort).findAll(pageable);
    }

    @Test
    @DisplayName("Should convert projection to DTO correctly")
    void shouldConvertProjectionToDTOCorrectly() {
        // Given
        List<AuditEventProjection> projections = Arrays.asList(testProjection);
        Page<AuditEventProjection> page = new PageImpl<>(projections, pageable, 1);

        when(auditEventQueryPort.findAll(pageable)).thenReturn(page);

        // When
        Page<AuditEventDTO> result = auditQueryService.findAllEvents(pageable);

        // Then
        assertThat(result).isNotNull();
        AuditEventDTO dto = result.getContent().get(0);

        assertThat(dto.getId()).isEqualTo(testProjection.getId());
        assertThat(dto.getEventType()).isEqualTo(testProjection.getEventType());
        assertThat(dto.getEventCategory()).isEqualTo(testProjection.getEventCategory());
        assertThat(dto.getUserId()).isEqualTo(testProjection.getUserId());
        assertThat(dto.getUsername()).isEqualTo(testProjection.getUsername());
        assertThat(dto.getTargetEntityType()).isEqualTo(testProjection.getTargetEntityType());
        assertThat(dto.getTargetEntityId()).isEqualTo(testProjection.getTargetEntityId());
        assertThat(dto.getTargetEntityName()).isEqualTo(testProjection.getTargetEntityName());
        assertThat(dto.getIpAddress()).isEqualTo(testProjection.getIpAddress());
        assertThat(dto.getUserAgent()).isEqualTo(testProjection.getUserAgent());
        assertThat(dto.getDetails()).isEqualTo(testProjection.getDetails());
        assertThat(dto.isSuccess()).isEqualTo(testProjection.isSuccess());
        assertThat(dto.getErrorMessage()).isEqualTo(testProjection.getErrorMessage());
        assertThat(dto.getTimestamp()).isEqualTo(testProjection.getTimestamp());
    }
/*
    @Test
    @DisplayName("Should handle multiple event types in query")
    void shouldHandleMultipleEventTypesInQuery() {
        // Given
        AuditEventQuery query = AuditEventQuery.builder()
            .eventTypes(Arrays.asList(
                AuditEventType.USER_CREATED,
                AuditEventType.USER_UPDATED,
                AuditEventType.USER_DELETED
            ))
            .build();

        Page<AuditEventProjection> emptyPage = Page.empty(pageable);
        when(auditEventQueryPort.findByFilters(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(emptyPage);

        // When
        Page<AuditEventDTO> result = auditQueryService.findEvents(query, pageable);

        // Then
        assertThat(result).isNotNull();
        verify(auditEventQueryPort).findByFilters(
            eq(query.getEventTypes()),
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(pageable)
        );
    }*/
}
