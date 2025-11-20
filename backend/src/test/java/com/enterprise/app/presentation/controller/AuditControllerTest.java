package com.enterprise.app.presentation.controller;

import com.enterprise.app.application.dto.audit.AuditEventDTO;
import com.enterprise.app.application.dto.audit.AuditEventQuery;
import com.enterprise.app.application.dto.audit.AuditStatisticsDTO;
import com.enterprise.app.application.service.audit.AuditQueryService;
import com.enterprise.app.domain.model.AuditEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuditController.
 */
@WebMvcTest(AuditController.class)
@DisplayName("AuditController Tests")
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditQueryService auditQueryService;

    private AuditEventDTO testEventDTO;
    private UUID testUserId;
    private UUID testEntityId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEntityId = UUID.randomUUID();

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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get all audit events with filters")
    void shouldGetAllAuditEventsWithFilters() throws Exception {
        // Given
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events);

        when(auditQueryService.findEvents(any(AuditEventQuery.class), any(Pageable.class)))
            .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/audit")
                .param("eventCategory", "USER")
                .param("success", "true")
                .param("page", "0")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(testEventDTO.getId().toString()))
            .andExpect(jsonPath("$.content[0].eventCategory").value("USER"));

        verify(auditQueryService).findEvents(any(AuditEventQuery.class), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get audit events without filters")
    void shouldGetAuditEventsWithoutFilters() throws Exception {
        // Given
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events);

        when(auditQueryService.findEvents(any(AuditEventQuery.class), any(Pageable.class)))
            .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/audit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].eventType").value("USER_CREATED"));

        verify(auditQueryService).findEvents(any(AuditEventQuery.class), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get audit events by user")
    void shouldGetAuditEventsByUser() throws Exception {
        // Given
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events);

        when(auditQueryService.findEventsByUser(eq(testUserId), any(Pageable.class)))
            .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/audit/user/{userId}", testUserId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].userId").value(testUserId.toString()));

        verify(auditQueryService).findEventsByUser(eq(testUserId), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get audit events by type")
    void shouldGetAuditEventsByType() throws Exception {
        // Given
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events);

        when(auditQueryService.findEventsByType(eq(AuditEventType.USER_CREATED), any(Pageable.class)))
            .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/audit/type/{eventType}", "USER_CREATED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].eventType").value("USER_CREATED"));

        verify(auditQueryService).findEventsByType(eq(AuditEventType.USER_CREATED), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get audit events by entity")
    void shouldGetAuditEventsByEntity() throws Exception {
        // Given
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events);

        when(auditQueryService.findEventsByTargetEntity(
            eq("User"), eq(testEntityId), any(Pageable.class)
        )).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/audit/entity/{entityType}/{entityId}", "User", testEntityId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].targetEntityType").value("User"))
            .andExpect(jsonPath("$.content[0].targetEntityId").value(testEntityId.toString()));

        verify(auditQueryService).findEventsByTargetEntity(
            eq("User"), eq(testEntityId), any(Pageable.class)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get audit statistics")
    void shouldGetAuditStatistics() throws Exception {
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

        // When & Then
        mockMvc.perform(get("/api/v1/audit/statistics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEvents").value(100))
            .andExpect(jsonPath("$.successfulEvents").value(80))
            .andExpect(jsonPath("$.failedEvents").value(20))
            .andExpect(jsonPath("$.eventsByType.USER_CREATED").value(50))
            .andExpect(jsonPath("$.eventsByCategory.USER").value(80));

        verify(auditQueryService).getStatistics();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get statistics for date range")
    void shouldGetStatisticsForDateRange() throws Exception {
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

        // When & Then
        mockMvc.perform(get("/api/v1/audit/statistics/range")
                .param("fromDate", fromDate.toString())
                .param("toDate", toDate.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEvents").value(50))
            .andExpect(jsonPath("$.eventsByDate").exists());

        verify(auditQueryService).getStatisticsForDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get hourly statistics")
    void shouldGetHourlyStatistics() throws Exception {
        // Given
        LocalDate date = LocalDate.now();
        Map<String, Long> hourlyStats = new HashMap<>();
        hourlyStats.put("9", 10L);
        hourlyStats.put("10", 15L);
        hourlyStats.put("11", 20L);

        when(auditQueryService.getHourlyStatistics(any(LocalDate.class)))
            .thenReturn(hourlyStats);

        // When & Then
        mockMvc.perform(get("/api/v1/audit/statistics/hourly")
                .param("date", date.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.9").value(10))
            .andExpect(jsonPath("$.10").value(15))
            .andExpect(jsonPath("$.11").value(20));

        verify(auditQueryService).getHourlyStatistics(any(LocalDate.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when user without required role tries to access audit")
    void shouldReturn403ForNonAuthorizedUser() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/audit"))
            .andExpect(status().isForbidden());

        verify(auditQueryService, never()).findEvents(any(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Should allow MANAGER to access audit events")
    void shouldAllowManagerToAccessAudit() throws Exception {
        // Given
        Page<AuditEventDTO> emptyPage = Page.empty();
        when(auditQueryService.findEvents(any(AuditEventQuery.class), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/v1/audit"))
            .andExpect(status().isOk());

        verify(auditQueryService).findEvents(any(AuditEventQuery.class), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "TECH_LEAD")
    @DisplayName("Should allow TECH_LEAD to access audit events")
    void shouldAllowTechLeadToAccessAudit() throws Exception {
        // Given
        Page<AuditEventDTO> emptyPage = Page.empty();
        when(auditQueryService.findEvents(any(AuditEventQuery.class), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/v1/audit"))
            .andExpect(status().isOk());

        verify(auditQueryService).findEvents(any(AuditEventQuery.class), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle multiple event types filter")
    void shouldHandleMultipleEventTypesFilter() throws Exception {
        // Given
        Page<AuditEventDTO> emptyPage = Page.empty();
        when(auditQueryService.findEvents(any(AuditEventQuery.class), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/v1/audit")
                .param("eventTypes", "USER_CREATED", "USER_UPDATED", "USER_DELETED"))
            .andExpect(status().isOk());

        verify(auditQueryService).findEvents(any(AuditEventQuery.class), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return empty page when no events found")
    void shouldReturnEmptyPageWhenNoEventsFound() throws Exception {
        // Given
        Page<AuditEventDTO> emptyPage = Page.empty();
        when(auditQueryService.findEventsByUser(eq(testUserId), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/v1/audit/user/{userId}", testUserId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0));

        verify(auditQueryService).findEventsByUser(eq(testUserId), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle pagination parameters")
    void shouldHandlePaginationParameters() throws Exception {
        // Given
        List<AuditEventDTO> events = Arrays.asList(testEventDTO);
        Page<AuditEventDTO> page = new PageImpl<>(events, PageRequest.of(1, 10), 100);

        when(auditQueryService.findEvents(any(AuditEventQuery.class), any(Pageable.class)))
            .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/audit")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(100));

        verify(auditQueryService).findEvents(any(AuditEventQuery.class), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should filter by success status")
    void shouldFilterBySuccessStatus() throws Exception {
        // Given
        Page<AuditEventDTO> emptyPage = Page.empty();
        when(auditQueryService.findEvents(any(AuditEventQuery.class), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/v1/audit")
                .param("success", "false"))
            .andExpect(status().isOk());

        verify(auditQueryService).findEvents(any(AuditEventQuery.class), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should filter by target entity type")
    void shouldFilterByTargetEntityType() throws Exception {
        // Given
        Page<AuditEventDTO> emptyPage = Page.empty();
        when(auditQueryService.findEvents(any(AuditEventQuery.class), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/v1/audit")
                .param("targetEntityType", "User"))
            .andExpect(status().isOk());

        verify(auditQueryService).findEvents(any(AuditEventQuery.class), any(Pageable.class));
    }
}
