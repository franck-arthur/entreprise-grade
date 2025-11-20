package com.enterprise.app.application.dto;

import com.enterprise.app.domain.model.BatchImportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for batch import responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportResponse {
    private UUID id;
    private String fileName;
    private Long fileSize;
    private BatchImportStatus status;
    private int totalLines;
    private int processedLines;
    private int successLines;
    private int failedLines;
    private double progressPercentage;
    private String errorMessage;
    private UUID initiatedByUserId;
    private String initiatedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
