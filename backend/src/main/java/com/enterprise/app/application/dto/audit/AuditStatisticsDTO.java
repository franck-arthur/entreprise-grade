package com.enterprise.app.application.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for audit statistics and aggregations.
 * Used for dashboard and analytics (Read side).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditStatisticsDTO {
    private long totalEvents;
    private long successfulEvents;
    private long failedEvents;
    private Map<String, Long> eventsByType;
    private Map<String, Long> eventsByCategory;
    private Map<String, Long> eventsByHour;
    private Map<String, Long> eventsByDate;
    private Map<String, Long> topUsers;
    private Map<String, Long> topTargetEntities;
}
