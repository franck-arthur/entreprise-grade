package com.enterprise.app.domain.port;

import com.enterprise.app.domain.model.BatchImport;
import com.enterprise.app.domain.model.BatchImportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for batch import persistence operations.
 * Follows hexagonal architecture pattern.
 */
public interface BatchImportPort {

    /**
     * Save a batch import.
     */
    BatchImport save(BatchImport batchImport);

    /**
     * Find a batch import by ID.
     */
    Optional<BatchImport> findById(UUID id);

    /**
     * Find all batch imports with pagination.
     */
    Page<BatchImport> findAll(Pageable pageable);

    /**
     * Find batch imports by status.
     */
    List<BatchImport> findByStatus(BatchImportStatus status);

    /**
     * Find batch imports by initiated user.
     */
    Page<BatchImport> findByInitiatedByUserId(UUID userId, Pageable pageable);

    /**
     * Delete a batch import.
     */
    void delete(BatchImport batchImport);

    /**
     * Check if a batch import exists.
     */
    boolean existsById(UUID id);
}
