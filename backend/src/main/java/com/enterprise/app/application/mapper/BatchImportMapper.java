package com.enterprise.app.application.mapper;

import com.enterprise.app.application.dto.BatchImportDetailResponse;
import com.enterprise.app.application.dto.BatchImportResponse;
import com.enterprise.app.domain.model.BatchImport;
import com.enterprise.app.domain.model.BatchImportLine;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper for converting BatchImport entities to DTOs.
 */
@Component
public class BatchImportMapper {

    /**
     * Convert BatchImport entity to BatchImportResponse DTO.
     */
    public BatchImportResponse toResponse(BatchImport batchImport) {
        return BatchImportResponse.builder()
            .id(batchImport.getId())
            .fileName(batchImport.getFileName())
            .fileSize(batchImport.getFileSize())
            .status(batchImport.getStatus())
            .totalLines(batchImport.getTotalLines())
            .processedLines(batchImport.getProcessedLines())
            .successLines(batchImport.getSuccessLines())
            .failedLines(batchImport.getFailedLines())
            .progressPercentage(batchImport.getProgressPercentage())
            .errorMessage(batchImport.getErrorMessage())
            .initiatedByUserId(batchImport.getInitiatedBy() != null
                ? batchImport.getInitiatedBy().getId() : null)
            .initiatedByUsername(batchImport.getInitiatedBy() != null
                ? batchImport.getInitiatedBy().getUsername() : null)
            .createdAt(batchImport.getCreatedAt())
            .updatedAt(batchImport.getUpdatedAt())
            .startedAt(batchImport.getStartedAt())
            .completedAt(batchImport.getCompletedAt())
            .build();
    }

    /**
     * Convert BatchImport entity to BatchImportDetailResponse DTO with lines.
     */
    public BatchImportDetailResponse toDetailResponse(BatchImport batchImport) {
        return BatchImportDetailResponse.builder()
            .id(batchImport.getId())
            .fileName(batchImport.getFileName())
            .fileSize(batchImport.getFileSize())
            .status(batchImport.getStatus())
            .totalLines(batchImport.getTotalLines())
            .processedLines(batchImport.getProcessedLines())
            .successLines(batchImport.getSuccessLines())
            .failedLines(batchImport.getFailedLines())
            .progressPercentage(batchImport.getProgressPercentage())
            .errorMessage(batchImport.getErrorMessage())
            .initiatedByUserId(batchImport.getInitiatedBy() != null
                ? batchImport.getInitiatedBy().getId() : null)
            .initiatedByUsername(batchImport.getInitiatedBy() != null
                ? batchImport.getInitiatedBy().getUsername() : null)
            .createdAt(batchImport.getCreatedAt())
            .updatedAt(batchImport.getUpdatedAt())
            .startedAt(batchImport.getStartedAt())
            .completedAt(batchImport.getCompletedAt())
            .lines(batchImport.getLines() != null
                ? batchImport.getLines().stream()
                    .map(this::toLineResponse)
                    .collect(Collectors.toList())
                : null)
            .build();
    }

    /**
     * Convert BatchImportLine entity to BatchImportLineResponse DTO.
     */
    private BatchImportDetailResponse.BatchImportLineResponse toLineResponse(
        BatchImportLine line) {

        return BatchImportDetailResponse.BatchImportLineResponse.builder()
            .id(line.getId())
            .lineNumber(line.getLineNumber())
            .rawData(line.getRawData())
            .success(line.isSuccess())
            .errorMessage(line.getErrorMessage())
            .createdUserId(line.getCreatedUser() != null
                ? line.getCreatedUser().getId() : null)
            .createdUsername(line.getCreatedUser() != null
                ? line.getCreatedUser().getUsername() : null)
            .build();
    }

}
