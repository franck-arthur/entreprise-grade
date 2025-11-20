package com.enterprise.app.infrastructure.persistence.adapter;

import com.enterprise.app.domain.model.BatchImport;
import com.enterprise.app.domain.model.BatchImportStatus;
import com.enterprise.app.domain.port.BatchImportPort;
import com.enterprise.app.infrastructure.persistence.repository.BatchImportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementation for BatchImportPort.
 * Bridges the domain layer with the infrastructure persistence layer.
 */
@Component
@RequiredArgsConstructor
public class BatchImportAdapter implements BatchImportPort {

    private final BatchImportRepository batchImportRepository;

    @Override
    public BatchImport save(BatchImport batchImport) {
        return batchImportRepository.save(batchImport);
    }

    @Override
    public Optional<BatchImport> findById(UUID id) {
        return batchImportRepository.findById(id);
    }

    @Override
    public Page<BatchImport> findAll(Pageable pageable) {
        return batchImportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public List<BatchImport> findByStatus(BatchImportStatus status) {
        return batchImportRepository.findByStatus(status);
    }

    @Override
    public Page<BatchImport> findByInitiatedByUserId(UUID userId, Pageable pageable) {
        return batchImportRepository.findByInitiatedByUserId(userId, pageable);
    }

    @Override
    public void delete(BatchImport batchImport) {
        batchImportRepository.delete(batchImport);
    }

    @Override
    public boolean existsById(UUID id) {
        return batchImportRepository.existsById(id);
    }
}
