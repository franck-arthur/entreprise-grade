package com.enterprise.app.application.dto;

import com.enterprise.app.domain.model.BatchImportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for detailed batch import responses including individual lines.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportDetailResponse {
    private Long id;
    private String fileName;
    private Long fileSize;
    private BatchImportStatus status;
    private int totalLines;
    private int processedLines;
    private int successLines;
    private int failedLines;
    private double progressPercentage;
    private String errorMessage;
    private Long initiatedByUserId;
    private String initiatedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<BatchImportLineResponse> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchImportLineResponse {
        private Long id;
        private int lineNumber;
        private String rawData;
        private boolean success;
        private String errorMessage;
        private Long createdUserId;
        private String createdUsername;
    }
}
