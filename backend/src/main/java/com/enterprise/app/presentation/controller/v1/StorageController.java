package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Storage operations.
 *
 * Provides endpoints for file upload, download, and management using S3-compatible storage (Garage).
 */
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Storage", description = "File storage management API")
@SecurityRequirement(name = "bearerAuth")
public class StorageController {

    private final StorageService storageService;

    /**
     * Upload a file to storage.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER', 'DEVELOPER')")
    @Operation(
        summary = "Upload file to storage",
        description = "Upload a file to S3-compatible storage. Returns the storage key for later retrieval."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or request"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Upload failed")
    })
    public ResponseEntity<Map<String, String>> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Optional folder path for organization")
            @RequestParam(value = "folder", required = false) String folder) {

        log.info("Uploading file: {} to folder: {}", file.getOriginalFilename(), folder);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
        }

        try {
            String key = storageService.uploadFile(file, folder);
            return ResponseEntity.ok(Map.of(
                    "key", key,
                    "filename", file.getOriginalFilename(),
                    "size", String.valueOf(file.getSize()),
                    "contentType", file.getContentType() != null ? file.getContentType() : "unknown"
            ));
        } catch (RuntimeException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * Download a file from storage.
     */
    @GetMapping("/download/{key}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER', 'DEVELOPER')")
    @Operation(
        summary = "Download file from storage",
        description = "Download a file using its storage key."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "500", description = "Download failed")
    })
    public ResponseEntity<InputStreamResource> downloadFile(
            @Parameter(description = "Storage key of the file", required = true)
            @PathVariable String key) {

        log.info("Downloading file with key: {}", key);

        try {
            InputStream fileStream = storageService.downloadFile(key);

            // Extract filename from key for download
            String filename = extractFilenameFromKey(key);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(fileStream));

        } catch (RuntimeException e) {
            log.error("Failed to download file with key: {}", key, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a file from storage.
     */
    @DeleteMapping("/{key}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD')")
    @Operation(
        summary = "Delete file from storage",
        description = "Delete a file using its storage key. Requires ADMIN or TECH_LEAD role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "500", description = "Delete failed")
    })
    public ResponseEntity<Map<String, String>> deleteFile(
            @Parameter(description = "Storage key of the file", required = true)
            @PathVariable String key) {

        log.info("Deleting file with key: {}", key);

        try {
            storageService.deleteFile(key);
            return ResponseEntity.ok(Map.of("message", "File deleted successfully", "key", key));
        } catch (RuntimeException e) {
            log.error("Failed to delete file with key: {}", key, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }

    /**
     * List files in storage.
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER')")
    @Operation(
        summary = "List files in storage",
        description = "List all files or files in a specific folder."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Files listed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "List failed")
    })
    public ResponseEntity<Map<String, Object>> listFiles(
            @Parameter(description = "Optional folder path to list files from")
            @RequestParam(value = "folder", required = false) String folder) {

        log.info("Listing files in folder: {}", folder);

        try {
            List<String> files = folder != null ? storageService.listFiles(folder) : storageService.listAllFiles();
            return ResponseEntity.ok(Map.of(
                "files", files,
                "count", files.size(),
                "folder", folder != null ? folder : "root"
            ));
        } catch (RuntimeException e) {
            log.error("Failed to list files in folder: {}", folder, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list files: " + e.getMessage()));
        }
    }

    /**
     * Check if a file exists in storage.
     */
    @GetMapping("/exists/{key}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECH_LEAD', 'MANAGER', 'DEVELOPER')")
    @Operation(
        summary = "Check if file exists",
        description = "Check if a file exists using its storage key."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check completed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> fileExists(
            @Parameter(description = "Storage key of the file", required = true)
            @PathVariable String key) {

        log.info("Checking existence of file with key: {}", key);

        boolean exists = storageService.fileExists(key);
        return ResponseEntity.ok(Map.of(
            "exists", exists,
            "key", key
        ));
    }

    /**
     * Extract filename from storage key.
     */
    private String extractFilenameFromKey(String key) {
        if (key == null || key.isEmpty()) {
            return "download";
        }

        // Extract the last part after the last '/'
        String[] parts = key.split("/");
        String lastPart = parts[parts.length - 1];

        // Remove UUID prefix if present (format: uuid-filename)
        if (lastPart.contains("-") && lastPart.length() > 9) {
            int firstDash = lastPart.indexOf('-');
            if (firstDash == 8) { // UUID prefix is 8 chars
                return lastPart.substring(firstDash + 1);
            }
        }

        return lastPart.isEmpty() ? "download" : lastPart;
    }
}