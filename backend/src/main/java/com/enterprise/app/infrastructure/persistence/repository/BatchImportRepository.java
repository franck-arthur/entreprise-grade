package com.enterprise.app.infrastructure.persistence.repository;

import com.enterprise.app.domain.model.BatchImport;
import com.enterprise.app.domain.model.BatchImportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository for BatchImport entities.
 */
@Repository
public interface BatchImportRepository extends JpaRepository<BatchImport, Long> {

    /**
     * Find batch imports by status.
     */
    List<BatchImport> findByStatus(BatchImportStatus status);

    /**
     * Find batch imports by initiated user with pagination.
     */
    @Query("SELECT bi FROM BatchImport bi WHERE bi.initiatedBy.id = :userId")
    Page<BatchImport> findByInitiatedByUserId(Long userId, Pageable pageable);

    /**
     * Find all batch imports ordered by creation date descending.
     */
    Page<BatchImport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
