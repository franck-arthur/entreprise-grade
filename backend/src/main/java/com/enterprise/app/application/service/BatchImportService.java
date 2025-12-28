package com.enterprise.app.application.service;

import com.enterprise.app.application.dto.CsvUserLine;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.domain.port.BatchImportPort;
import com.enterprise.app.domain.repository.UserRepository;
import com.enterprise.app.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Service for handling batch import operations with multithreading.
 * Processes CSV files in parallel for optimal performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchImportService {

    private final BatchImportPort batchImportPort;
    private final UserRepository userRepository;
    private final MessageSource messageSource;
    private final Executor csvProcessorExecutor;

    /**
     * Create a new batch import from a CSV file.
     * This method creates the BatchImport entity and initiates async processing.
     */
    @Transactional
    public BatchImport createBatchImport(MultipartFile file, User initiatedBy) {
        log.info("Creating batch import for file: {}", file.getOriginalFilename());

        BatchImport batchImport = BatchImport.builder()
            .fileName(file.getOriginalFilename())
            .fileSize(file.getSize())
            .status(BatchImportStatus.PENDING)
            .initiatedBy(initiatedBy)
            .build();

        BatchImport savedBatchImport = batchImportPort.save(batchImport);

        // Start async processing
        processFileAsync(savedBatchImport.getId(), file);

        return savedBatchImport;
    }

    /**
     * Process the CSV file asynchronously.
     * This method runs in a separate thread to avoid blocking the main request.
     */
    @Async("batchImportExecutor")
    public void processFileAsync(Long batchImportId, MultipartFile file) {
        log.info("Starting async processing for batch import: {}", batchImportId);

        try {
            // Reload the batch import in this transaction
            BatchImport batchImport = batchImportPort.findById(batchImportId)
                .orElseThrow(() -> new ResourceNotFoundException("BatchImport", batchImportId));

            batchImport.start();
            batchImportPort.save(batchImport);

            // Parse CSV file
            List<CsvUserLine> csvLines = parseCsvFile(file);
            batchImport.setTotalLines(csvLines.size());
            batchImportPort.save(batchImport);

            // Process lines in parallel using CompletableFuture
            List<CompletableFuture<BatchImportLine>> futures = csvLines.stream()
                .map(csvLine -> processLineAsync(batchImport, csvLine))
                .collect(Collectors.toList());

            // Wait for all futures to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            allFutures.join(); // Wait for all lines to be processed

            // Collect results
            List<BatchImportLine> processedLines = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

            // Update batch import with results
            updateBatchImportResults(batchImportId, processedLines);

        } catch (Exception e) {
            log.error("Error processing batch import: {}", batchImportId, e);
            failBatchImport(batchImportId, e.getMessage());
        }
    }

    /**
     * Parse CSV file into list of CsvUserLine objects.
     */
    private List<CsvUserLine> parseCsvFile(MultipartFile file) throws Exception {
        List<CsvUserLine> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip header line
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",", -1); // -1 to include empty trailing fields

                CsvUserLine csvLine = CsvUserLine.builder()
                    .lineNumber(lineNumber)
                    .rawLine(line)
                    .username(parts.length > 0 ? parts[0].trim() : "")
                    .email(parts.length > 1 ? parts[1].trim() : "")
                    .firstName(parts.length > 2 ? parts[2].trim() : "")
                    .lastName(parts.length > 3 ? parts[3].trim() : "")
                    .phoneNumber(parts.length > 4 ? parts[4].trim() : "")
                    .roles(parts.length > 5 ? parts[5].trim() : "USER")
                    .build();

                lines.add(csvLine);
            }
        }

        log.info("Parsed {} lines from CSV file", lines.size());
        return lines;
    }

    /**
     * Process a single CSV line asynchronously.
     * This method runs in the CSV processor thread pool.
     */
    private CompletableFuture<BatchImportLine> processLineAsync(
        BatchImport batchImport, CsvUserLine csvLine) {

        return CompletableFuture.supplyAsync(() -> {
            BatchImportLine importLine = BatchImportLine.builder()
                .batchImport(batchImport)
                .lineNumber(csvLine.getLineNumber())
                .rawData(csvLine.getRawLine())
                .build();

            try {
                // Validate CSV line
                validateCsvLine(csvLine);

                // Check if user already exists
                if (userRepository.findByUsername(csvLine.getUsername()).isPresent()) {
                    throw new IllegalArgumentException(
                        getMessage("error.batch.user.already.exists",
                            new Object[]{csvLine.getUsername()})
                    );
                }

                if (userRepository.findByEmail(csvLine.getEmail()).isPresent()) {
                    throw new IllegalArgumentException(
                        getMessage("error.batch.email.already.exists",
                            new Object[]{csvLine.getEmail()})
                    );
                }

                // Parse roles
                Set<Role> roles = parseRoles(csvLine.getRoles());

                // Create user
                User user = User.builder()
                    .username(csvLine.getUsername())
                    .email(csvLine.getEmail())
                    .firstName(csvLine.getFirstName())
                    .lastName(csvLine.getLastName())
                    .phoneNumber(csvLine.getPhoneNumber())
                    .roles(roles)
                    .active(true)
                    .emailVerified(false)
                    .build();

                User savedUser = userRepository.save(user);
                importLine.markAsSuccess(savedUser);

                log.debug("Successfully processed line {} - User: {}",
                    csvLine.getLineNumber(), csvLine.getUsername());

            } catch (Exception e) {
                importLine.markAsFailure(e.getMessage());
                log.warn("Failed to process line {} - Error: {}",
                    csvLine.getLineNumber(), e.getMessage());
            }

            return importLine;
        }, csvProcessorExecutor);
    }

    /**
     * Validate a CSV line.
     */
    private void validateCsvLine(CsvUserLine csvLine) {
        if (csvLine.getUsername() == null || csvLine.getUsername().isEmpty()) {
            throw new IllegalArgumentException(
                getMessage("error.batch.username.required", null)
            );
        }

        if (csvLine.getEmail() == null || csvLine.getEmail().isEmpty()) {
            throw new IllegalArgumentException(
                getMessage("error.batch.email.required", null)
            );
        }

        // Basic email validation
        if (!csvLine.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException(
                getMessage("error.batch.email.invalid", new Object[]{csvLine.getEmail()})
            );
        }

        // Username length validation
        if (csvLine.getUsername().length() < 3 || csvLine.getUsername().length() > 50) {
            throw new IllegalArgumentException(
                getMessage("error.batch.username.size", null)
            );
        }
    }

    /**
     * Parse roles from comma-separated string.
     */
    private Set<Role> parseRoles(String rolesStr) {
        Set<Role> roles = new HashSet<>();

        if (rolesStr == null || rolesStr.isEmpty()) {
            roles.add(Role.USER);
            return roles;
        }

        String[] roleParts = rolesStr.split(",");
        for (String rolePart : roleParts) {
            String roleName = rolePart.trim().toUpperCase();
            try {
                roles.add(Role.valueOf(roleName));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role: {}. Defaulting to USER", roleName);
                roles.add(Role.USER);
            }
        }

        return roles;
    }

    /**
     * Update batch import with processing results.
     */
    @Transactional
    public void updateBatchImportResults(Long batchImportId, List<BatchImportLine> lines) {
        BatchImport batchImport = batchImportPort.findById(batchImportId)
            .orElseThrow(() -> new ResourceNotFoundException("BatchImport", batchImportId));

        // Add all lines
        for (BatchImportLine line : lines) {
            batchImport.addLine(line);
            if (line.isSuccess()) {
                batchImport.incrementSuccess();
            } else {
                batchImport.incrementFailed();
            }
        }

        batchImport.complete();
        batchImportPort.save(batchImport);

        log.info("Batch import {} completed. Success: {}, Failed: {}",
            batchImportId, batchImport.getSuccessLines(), batchImport.getFailedLines());
    }

    /**
     * Mark batch import as failed.
     */
    @Transactional
    public void failBatchImport(Long batchImportId, String errorMessage) {
        BatchImport batchImport = batchImportPort.findById(batchImportId)
            .orElseThrow(() -> new ResourceNotFoundException("BatchImport", batchImportId));

        batchImport.fail(errorMessage);
        batchImportPort.save(batchImport);
    }

    /**
     * Get batch import by ID.
     */
    @Transactional(readOnly = true)
    public BatchImport getBatchImportById(Long id) {
        return batchImportPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BatchImport", id));
    }

    /**
     * Get all batch imports with pagination.
     */
    @Transactional(readOnly = true)
    public Page<BatchImport> getAllBatchImports(Pageable pageable) {
        return batchImportPort.findAll(pageable);
    }

    /**
     * Get batch imports for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<BatchImport> getBatchImportsByUser(Long userId, Pageable pageable) {
        return batchImportPort.findByInitiatedByUserId(userId, pageable);
    }

    /**
     * Cancel a batch import.
     */
    @Transactional
    public void cancelBatchImport(Long id) {
        BatchImport batchImport = batchImportPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BatchImport", id));

        if (batchImport.isInProgress()) {
            batchImport.cancel();
            batchImportPort.save(batchImport);
            log.info("Batch import {} cancelled", id);
        }
    }

    /**
     * Get localized message.
     */
    private String getMessage(String key, Object[] args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
