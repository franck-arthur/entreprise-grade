package com.enterprise.app.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a batch import operation.
 * Tracks the overall status and results of importing a CSV file.
 */
@Entity
@Table(name = "batch_imports")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BatchImportStatus status = BatchImportStatus.PENDING;

    @Column(name = "total_lines")
    @Builder.Default
    private int totalLines = 0;

    @Column(name = "processed_lines")
    @Builder.Default
    private int processedLines = 0;

    @Column(name = "success_lines")
    @Builder.Default
    private int successLines = 0;

    @Column(name = "failed_lines")
    @Builder.Default
    private int failedLines = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by_user_id")
    private User initiatedBy;

    @OneToMany(mappedBy = "batchImport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BatchImportLine> lines = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    private Long version;

    /**
     * Start the batch import processing.
     */
    public void start() {
        this.status = BatchImportStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * Complete the batch import successfully.
     */
    public void complete() {
        this.status = this.failedLines > 0
            ? BatchImportStatus.COMPLETED_WITH_ERRORS
            : BatchImportStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark the batch import as failed.
     */
    public void fail(String errorMessage) {
        this.status = BatchImportStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Cancel the batch import.
     */
    public void cancel() {
        this.status = BatchImportStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Add a line to the import.
     */
    public void addLine(BatchImportLine line) {
        if (this.lines == null) {
            this.lines = new ArrayList<>();
        }
        this.lines.add(line);
        line.setBatchImport(this);
    }

    /**
     * Increment the processed lines counter.
     */
    public void incrementProcessed() {
        this.processedLines++;
    }

    /**
     * Increment the success lines counter.
     */
    public void incrementSuccess() {
        this.successLines++;
        incrementProcessed();
    }

    /**
     * Increment the failed lines counter.
     */
    public void incrementFailed() {
        this.failedLines++;
        incrementProcessed();
    }

    /**
     * Calculate the progress percentage.
     */
    public double getProgressPercentage() {
        if (totalLines == 0) {
            return 0.0;
        }
        return (double) processedLines / totalLines * 100.0;
    }

    /**
     * Check if the import is still in progress.
     */
    public boolean isInProgress() {
        return status == BatchImportStatus.PROCESSING;
    }

    /**
     * Check if the import is complete (any terminal status).
     */
    public boolean isCompleted() {
        return status == BatchImportStatus.COMPLETED
            || status == BatchImportStatus.COMPLETED_WITH_ERRORS
            || status == BatchImportStatus.FAILED
            || status == BatchImportStatus.CANCELLED;
    }
}
