package com.enterprise.app.infrastructure.storage;

import com.enterprise.app.domain.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileWorkflowManagerImpl implements FileWorkflowManager {

    private final LocalFileManager localFileManager;
    private final FileStorage fileStorage;

    private final Map<String, FileJob> activeJobs = new ConcurrentHashMap<>();
    private static final DateTimeFormatter FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public FileJob createFileJob(String projectName, Set<String> fileNames) {
        String jobId = generateJobId();
        String directoryPath = generateProjectPath(projectName, jobId);

        log.info("Creating file job {} for project {} with {} files", jobId, projectName, fileNames.size());

        try {
            Path localDirectory = localFileManager.createDirectoryWithEmptyFiles(directoryPath, fileNames);

            FileJob job = new FileJob(
                jobId,
                projectName,
                localDirectory,
                fileNames,
                FileJobStatus.CREATED
            );

            activeJobs.put(jobId, job);

            log.info("Successfully created file job: {}", job);
            return job;

        } catch (Exception e) {
            log.error("Failed to create file job {} for project {}", jobId, projectName, e);
            throw new RuntimeException("Failed to create file job", e);
        }
    }

    @Override
    public boolean areFilesReady(String jobId) {
        FileJob job = getJobInfo(jobId);

        log.debug("Checking if files are ready for job: {}", jobId);

        try {
            for (String fileName : job.fileNames()) {
                Path filePath = job.localDirectory().resolve(fileName);

                if (!localFileManager.fileExists(filePath.toString())) {
                    log.warn("File does not exist: {}", filePath);
                    return false;
                }

                if (localFileManager.isFileEmpty(filePath.toString())) {
                    log.debug("File is still empty: {}", filePath);
                    return false;
                }
            }

            log.info("All files are ready for job: {}", jobId);

            // Mettre à jour le statut du job
            FileJob updatedJob = new FileJob(
                job.jobId(),
                job.projectName(),
                job.localDirectory(),
                job.fileNames(),
                FileJobStatus.FILES_READY
            );
            activeJobs.put(jobId, updatedJob);

            return true;

        } catch (Exception e) {
            log.error("Failed to check file readiness for job: {}", jobId, e);
            return false;
        }
    }

    @Override
    public List<String> uploadFilesToS3(String jobId) {
        FileJob job = getJobInfo(jobId);

        if (job.status() != FileJobStatus.FILES_READY) {
            throw new IllegalStateException("Files are not ready for job: " + jobId);
        }

        log.info("Uploading files to S3 for job: {}", jobId);

        List<String> uploadedKeys = new ArrayList<>();

        try {
            for (String fileName : job.fileNames()) {
                Path filePath = job.localDirectory().resolve(fileName);

                if (!localFileManager.fileExists(filePath.toString())) {
                    throw new IllegalStateException("File does not exist: " + filePath);
                }

                // Générer la clé S3
                String s3Key = generateS3Key(job.projectName(), fileName);

                // Upload vers S3
                try (FileInputStream fileInputStream = new FileInputStream(filePath.toFile())) {
                    long fileSize = localFileManager.getFileSize(filePath.toString());
                    String contentType = determineContentType(fileName);

                    String uploadedKey = fileStorage.uploadFile(fileInputStream, s3Key, contentType, fileSize);
                    uploadedKeys.add(uploadedKey);

                    log.debug("Uploaded file {} to S3 with key: {}", filePath, uploadedKey);
                }
            }

            // Mettre à jour le statut du job
            FileJob updatedJob = new FileJob(
                job.jobId(),
                job.projectName(),
                job.localDirectory(),
                job.fileNames(),
                FileJobStatus.UPLOADED_TO_S3
            );
            activeJobs.put(jobId, updatedJob);

            log.info("Successfully uploaded {} files to S3 for job: {}", uploadedKeys.size(), jobId);
            return uploadedKeys;

        } catch (Exception e) {
            log.error("Failed to upload files to S3 for job: {}", jobId, e);

            // Marquer le job comme échoué
            FileJob failedJob = new FileJob(
                job.jobId(),
                job.projectName(),
                job.localDirectory(),
                job.fileNames(),
                FileJobStatus.FAILED
            );
            activeJobs.put(jobId, failedJob);

            throw new RuntimeException("Failed to upload files to S3", e);
        }
    }

    @Override
    public void cleanupLocalFiles(String jobId) {
        FileJob job = getJobInfo(jobId);

        if (job.status() != FileJobStatus.UPLOADED_TO_S3) {
            log.warn("Cannot cleanup files for job {} with status: {}", jobId, job.status());
            return;
        }

        log.info("Cleaning up local files for job: {}", jobId);

        try {
            localFileManager.deleteDirectory(job.localDirectory().toString());

            // Mettre à jour le statut du job
            FileJob cleanedJob = new FileJob(
                job.jobId(),
                job.projectName(),
                job.localDirectory(),
                job.fileNames(),
                FileJobStatus.CLEANED_UP
            );
            activeJobs.put(jobId, cleanedJob);

            log.info("Successfully cleaned up local files for job: {}", jobId);

        } catch (Exception e) {
            log.error("Failed to cleanup local files for job: {}", jobId, e);
            throw new RuntimeException("Failed to cleanup local files", e);
        }
    }

    @Override
    public List<String> processCompleteWorkflow(String jobId) {
        log.info("Processing complete workflow for job: {}", jobId);

        FileJob job = getJobInfo(jobId);

        try {
            // Étape 1: Vérifier que les fichiers sont prêts
            if (!areFilesReady(jobId)) {
                throw new IllegalStateException("Files are not ready for job: " + jobId);
            }

            // Étape 2: Upload vers S3
            List<String> uploadedKeys = uploadFilesToS3(jobId);

            // Étape 3: Nettoyer les fichiers locaux
            cleanupLocalFiles(jobId);

            log.info("Successfully completed workflow for job: {}", jobId);
            return uploadedKeys;

        } catch (Exception e) {
            log.error("Failed to process complete workflow for job: {}", jobId, e);

            // Marquer le job comme échoué
            FileJob failedJob = new FileJob(
                job.jobId(),
                job.projectName(),
                job.localDirectory(),
                job.fileNames(),
                FileJobStatus.FAILED
            );
            activeJobs.put(jobId, failedJob);

            throw new RuntimeException("Failed to process complete workflow", e);
        }
    }

    @Override
    public FileJob getJobInfo(String jobId) {
        FileJob job = activeJobs.get(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }
        return job;
    }

    @Override
    public void deleteJob(String jobId) {
        log.info("Deleting job: {}", jobId);

        FileJob job = activeJobs.remove(jobId);
        if (job == null) {
            log.warn("Job not found for deletion: {}", jobId);
            return;
        }

        try {
            // Nettoyer les fichiers locaux si ils existent encore
            if (localFileManager.directoryExists(job.localDirectory().toString())) {
                localFileManager.deleteDirectory(job.localDirectory().toString());
            }

            log.info("Successfully deleted job: {}", jobId);

        } catch (Exception e) {
            log.error("Failed to cleanup resources for deleted job: {}", jobId, e);
            // Ne pas relancer l'exception car le job est déjà supprimé de la map
        }
    }

    @Override
    public List<FileJob> listActiveJobs() {
        return new ArrayList<>(activeJobs.values());
    }

    private String generateJobId() {
        return UUID.randomUUID().toString();
    }

    private String generateProjectPath(String projectName, String jobId) {
        String timestamp = LocalDateTime.now().format(FOLDER_FORMAT);
        String cleanName = cleanProjectName(projectName);
        String shortJobId = jobId.substring(0, 8);

        return String.format("temp-jobs/%s/%s-%s", timestamp, cleanName, shortJobId);
    }

    private String generateS3Key(String projectName, String fileName) {
        String timestamp = LocalDateTime.now().format(FOLDER_FORMAT);
        String cleanName = cleanProjectName(projectName);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        return String.format("projects/%s/%s-%s/%s", timestamp, cleanName, uniqueId, fileName);
    }

    private String cleanProjectName(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return "unknown-project";
        }

        return projectName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String determineContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".txt")) return "text/plain";
        if (lowerName.endsWith(".csv")) return "text/csv";
        if (lowerName.endsWith(".json")) return "application/json";
        if (lowerName.endsWith(".xml")) return "application/xml";
        if (lowerName.endsWith(".pdf")) return "application/pdf";
        if (lowerName.endsWith(".zip")) return "application/zip";

        return "application/octet-stream";
    }
}