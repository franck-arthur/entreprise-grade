package com.enterprise.app.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a single line in a batch import.
 * Tracks success/failure status and error messages for each line.
 */
@Entity
@Table(name = "batch_import_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchImportLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_import_id", nullable = false)
    private BatchImport batchImport;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    @Column(nullable = false)
    @Builder.Default
    private boolean success = false;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_user_id")
    private User createdUser;

    /**
     * Mark this line as successfully processed.
     */
    public void markAsSuccess(User createdUser) {
        this.success = true;
        this.errorMessage = null;
        this.createdUser = createdUser;
    }

    /**
     * Mark this line as failed with an error message.
     */
    public void markAsFailure(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.createdUser = null;
    }
}
