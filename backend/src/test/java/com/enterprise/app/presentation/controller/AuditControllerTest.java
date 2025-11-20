package com.enterprise.app.presentation.controller;

import com.enterprise.app.application.dto.audit.AuditEventDTO;
import com.enterprise.app.application.dto.audit.AuditStatisticsDTO;
import com.enterprise.app.application.service.audit.AuditQueryService;
import com.enterprise.app.domain.model.AuditEventType;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditController Tests")
class AuditControllerTest {

    @Mock
    private AuditQueryService auditQueryService;

    @InjectMocks
    private AuditController auditController;

    private AuditEventDTO testEventDTO;
    private UUID testUserId;
    private UUID testEntityId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEntityId = UUID.randomUUID();
        pageable = PageRequest.of(0, 50);

        testEventDTO = AuditEventDTO.builder()
            .id(UUID.randomUUID())
            .eventType(AuditEventType.USER_CREATED)
            .eventCategory("USER")
            .userId(testUserId)
            .username("testuser")
            .targetEntityType("User")
            .targetEntityId(testEntityId)
            .targetEntityName("John Doe")
            .ipAddress("192.168.1.100")
            .userAgent("Mozilla/5.0")
            .details("User created successfully")
            .success(true)
            .timestamp(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("Should get all audit events with filters")
    void shouldGetAllAuditEventsWithFilters() {
        // Given
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events);

        when(auditQueryService.findEvents(any(), any(Pageable.class))).thenReturn(page);

        // When
        ResponseEntity<Page<AuditEventDTO>> result = auditController.getAuditEvents(
            Arrays.asList(AuditEventType.USER_CREATED),
            "USER",
            testUserId,
            "testuser",
            "User",
            testEntityId,
            true,
            LocalDateTime.now().minusDays(7),
            LocalDateTime.now(),
            pageable
        );

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        assertThat(result.getBody().getContent().get(0).getId()).isEqualTo(testEventDTO.getId());
        assertThat(result.getBody().getContent().get(0).getEventCategory()).isEqualTo("USER");

        verify(auditQueryService).findEvents(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get audit events without filters")
    void shouldGetAuditEventsWithoutFilters() {
        // Given
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events);

        when(auditQueryService.findEvents(any(), any(Pageable.class))).thenReturn(page);

        // When
        ResponseEntity<Page<AuditEventDTO>> result = auditController.getAuditEvents(
            null, null, null, null, null, null, null, null, null, pageable
        );

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        assertThat(result.getBody().getContent().get(0).getEventType())
            .isEqualTo(AuditEventType.USER_CREATED);

        verify(auditQueryService).findEvents(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get audit events by user")
    void shouldGetAuditEventsByUser() {
        // Given
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events);

        when(auditQueryService.findEventsByUser(eq(testUserId), any(Pageable.class)))
            .thenReturn(page);

        // When
        ResponseEntity<Page<AuditEventDTO>> result =
            auditController.getAuditEventsByUser(testUserId, pageable);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        assertThat(result.getBody().getContent().get(0).getUserId()).isEqualTo(testUserId);

        verify(auditQueryService).findEventsByUser(eq(testUserId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get audit events by type")
    void shouldGetAuditEventsByType() {
        // Given
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events);

        when(auditQueryService.findEventsByType(eq(AuditEventType.USER_CREATED), any(Pageable.class)))
            .thenReturn(page);

        // When
        ResponseEntity<Page<AuditEventDTO>> result =
            auditController.getAuditEventsByType(AuditEventType.USER_CREATED, pageable);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        assertThat(result.getBody().getContent().get(0).getEventType())
            .isEqualTo(AuditEventType.USER_CREATED);

        verify(auditQueryService).findEventsByType(eq(AuditEventType.USER_CREATED), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get audit events by entity")
    void shouldGetAuditEventsByEntity() {
        // Given
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events);

        when(auditQueryService.findEventsByTargetEntity(
            eq("User"), eq(testEntityId), any(Pageable.class)
        )).thenReturn(page);

        // When
        ResponseEntity<Page<AuditEventDTO>> result =
            auditController.getAuditEventsByEntity("User", testEntityId, pageable);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        assertThat(result.getBody().getContent().get(0).getTargetEntityType()).isEqualTo("User");
        assertThat(result.getBody().getContent().get(0).getTargetEntityId()).isEqualTo(testEntityId);

        verify(auditQueryService).findEventsByTargetEntity(
            eq("User"), eq(testEntityId), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("Should get audit statistics")
    void shouldGetAuditStatistics() {
        // Given
        Map<String, Long> eventsByType = new HashMap<>();
        eventsByType.put("USER_CREATED", 50L);
        eventsByType.put("USER_UPDATED", 30L);

        Map<String, Long> eventsByCategory = new HashMap<>();
        eventsByCategory.put("USER", 80L);
        eventsByCategory.put("AUTH", 20L);

        AuditStatisticsDTO statistics = AuditStatisticsDTO.builder()
            .totalEvents(100L)
            .successfulEvents(80L)
            .failedEvents(20L)
            .eventsByType(eventsByType)
            .eventsByCategory(eventsByCategory)
            .build();

        when(auditQueryService.getStatistics()).thenReturn(statistics);

        // When
        ResponseEntity<AuditStatisticsDTO> result = auditController.getStatistics();

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTotalEvents()).isEqualTo(100);
        assertThat(result.getBody().getSuccessfulEvents()).isEqualTo(80);
        assertThat(result.getBody().getFailedEvents()).isEqualTo(20);
        assertThat(result.getBody().getEventsByType().get("USER_CREATED")).isEqualTo(50);
        assertThat(result.getBody().getEventsByCategory().get("USER")).isEqualTo(80);

        verify(auditQueryService).getStatistics();
    }

    @Test
    @DisplayName("Should get statistics for date range")
    void shouldGetStatisticsForDateRange() {
        // Given
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();

        Map<String, Long> eventsByDate = new HashMap<>();
        eventsByDate.put("2024-01-01", 20L);

        AuditStatisticsDTO statistics = AuditStatisticsDTO.builder()
            .totalEvents(50L)
            .eventsByDate(eventsByDate)
            .build();

        when(auditQueryService.getStatisticsForDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(statistics);

        // When
        ResponseEntity<AuditStatisticsDTO> result =
            auditController.getStatisticsForDateRange(fromDate, toDate);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTotalEvents()).isEqualTo(50);
        assertThat(result.getBody().getEventsByDate()).isNotNull();

        verify(auditQueryService).getStatisticsForDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should get hourly statistics")
    void shouldGetHourlyStatistics() {
        // Given
        LocalDate date = LocalDate.now();
        Map<String, Long> hourlyStats = new HashMap<>();
        hourlyStats.put("9", 10L);
        hourlyStats.put("10", 15L);
        hourlyStats.put("11", 20L);

        when(auditQueryService.getHourlyStatistics(any(LocalDate.class)))
            .thenReturn(hourlyStats);

        // When
        ResponseEntity<Map<String, Long>> result = auditController.getHourlyStatistics(date);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().get("9")).isEqualTo(10);
        assertThat(result.getBody().get("10")).isEqualTo(15);
        assertThat(result.getBody().get("11")).isEqualTo(20);

        verify(auditQueryService).getHourlyStatistics(any(LocalDate.class));
    }

    @Test
    @DisplayName("Should return empty page when no events found")
    void shouldReturnEmptyPageWhenNoEventsFound() {
        // Given
        Page<AuditEventDTO> emptyPage = Page.empty(pageable);
        when(auditQueryService.findEventsByUser(eq(testUserId), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When
        ResponseEntity<Page<AuditEventDTO>> result =
            auditController.getAuditEventsByUser(testUserId, pageable);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).isEmpty();
        assertThat(result.getBody().getTotalElements()).isZero();

        verify(auditQueryService).findEventsByUser(eq(testUserId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle pagination parameters")
    void shouldHandlePaginationParameters() {
        // Given
        Pageable customPageable = PageRequest.of(1, 10);
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events, customPageable, 100);

        when(auditQueryService.findEvents(any(), eq(customPageable))).thenReturn(page);

        // When
        ResponseEntity<Page<AuditEventDTO>> result = auditController.getAuditEvents(
            null, null, null, null, null, null, null, null, null, customPageable
        );

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        assertThat(result.getBody().getNumber()).isEqualTo(1);
        assertThat(result.getBody().getSize()).isEqualTo(10);
        assertThat(result.getBody().getTotalElements()).isEqualTo(100);

        verify(auditQueryService).findEvents(any(), eq(customPageable));
    }

    @Test
    @DisplayName("Should filter by success status")
    void shouldFilterBySuccessStatus() {
        // Given
        Page<AuditEventDTO> emptyPage = Page.empty();
        when(auditQueryService.findEvents(any(), any(Pageable.class))).thenReturn(emptyPage);

        // When
        ResponseEntity<Page<AuditEventDTO>> result = auditController.getAuditEvents(
            null, null, null, null, null, null, false, null, null, pageable
        );

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditQueryService).findEvents(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should filter by multiple event types")
    void shouldFilterByMultipleEventTypes() {
        // Given
        List<AuditEventType> eventTypes = Arrays.asList(
            AuditEventType.USER_CREATED,
            AuditEventType.USER_UPDATED,
            AuditEventType.USER_DELETED
        );
        Page<AuditEventDTO> emptyPage = Page.empty();
        when(auditQueryService.findEvents(any(), any(Pageable.class))).thenReturn(emptyPage);

        // When
        ResponseEntity<Page<AuditEventDTO>> result = auditController.getAuditEvents(
            eventTypes, null, null, null, null, null, null, null, null, pageable
        );

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditQueryService).findEvents(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle date range filters")
    void shouldHandleDateRangeFilters() {
        // Given
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();
        Page<AuditEventDTO> emptyPage = Page.empty();

        when(auditQueryService.findEvents(any(), any(Pageable.class))).thenReturn(emptyPage);

        // When
        ResponseEntity<Page<AuditEventDTO>> result = auditController.getAuditEvents(
            null, null, null, null, null, null, null, fromDate, toDate, pageable
        );

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditQueryService).findEvents(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return statistics with empty maps when no data")
    void shouldReturnStatisticsWithEmptyMaps() {
        // Given
        AuditStatisticsDTO emptyStats = AuditStatisticsDTO.builder()
            .totalEvents(0L)
            .successfulEvents(0L)
            .failedEvents(0L)
            .eventsByType(new HashMap<>())
            .eventsByCategory(new HashMap<>())
            .build();

        when(auditQueryService.getStatistics()).thenReturn(emptyStats);

        // When
        ResponseEntity<AuditStatisticsDTO> result = auditController.getStatistics();

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTotalEvents()).isZero();
        assertThat(result.getBody().getEventsByType()).isEmpty();

        verify(auditQueryService).getStatistics();
    }
}
