package com.enterprise.app.application.service;

import com.enterprise.app.domain.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class EnhancedStorageService {

    private final FileStorage fileStorage;
    private final DirectoryManager directoryManager;
    private final ZipService zipService;
    private final FileWorkflowManager workflowManager;
    private final LocalFileManager localFileManager;

    public EnhancedStorageService(FileStorage fileStorage,
                                DirectoryManager directoryManager,
                                ZipService zipService,
                                FileWorkflowManager workflowManager,
                                LocalFileManager localFileManager) {
        this.fileStorage = fileStorage;
        this.directoryManager = directoryManager;
        this.zipService = zipService;
        this.workflowManager = workflowManager;
        this.localFileManager = localFileManager;
    }

    private static final DateTimeFormatter FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public String createDirectoryWithFiles(String baseDirectoryName, Map<String, MultipartFile> files) {
        String directoryPath = generateDirectoryPath(baseDirectoryName);

        log.info("Creating directory with files: {}", directoryPath);

        try {
            directoryManager.createDirectory(directoryPath);

            Map<String, FileData> fileDataMap = new HashMap<>();
            for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
                String fileName = entry.getKey();
                MultipartFile file = entry.getValue();

                FileData fileData = new FileData(
                        file.getInputStream(),
                        file.getContentType(),
                        file.getSize()
                );

                fileDataMap.put(fileName, fileData);
            }

            directoryManager.addFilesToDirectory(directoryPath, fileDataMap);

            log.info("Successfully created directory with {} files: {}", files.size(), directoryPath);
            return directoryPath;

        } catch (Exception e) {
            log.error("Failed to create directory with files: {}", directoryPath, e);
            throw new RuntimeException("Failed to create directory with files", e);
        }
    }

    public String createAndZipDirectory(String baseDirectoryName, Map<String, MultipartFile> files, String zipFileName) {
        log.info("Creating directory, adding files, and zipping: {}", baseDirectoryName);

        try {
            String directoryPath = createDirectoryWithFiles(baseDirectoryName, files);

            String zipKey = zipService.uploadZippedDirectory(directoryPath, zipFileName);

            log.info("Successfully created and zipped directory. ZIP key: {}", zipKey);
            return zipKey;

        } catch (Exception e) {
            log.error("Failed to create and zip directory: {}", baseDirectoryName, e);
            throw new RuntimeException("Failed to create and zip directory", e);
        }
    }

    public String addFileToExistingDirectory(String directoryPath, String fileName, MultipartFile file) {
        log.info("Adding file {} to existing directory: {}", fileName, directoryPath);

        try {
            if (!directoryManager.directoryExists(directoryPath)) {
                throw new IllegalArgumentException("Directory does not exist: " + directoryPath);
            }

            directoryManager.addFileToDirectory(
                    directoryPath,
                    fileName,
                    file.getInputStream(),
                    file.getContentType()
            );

            String fileKey = ensureDirectoryPath(directoryPath) + fileName;
            log.info("Successfully added file to directory. File key: {}", fileKey);
            return fileKey;

        } catch (Exception e) {
            log.error("Failed to add file {} to directory: {}", fileName, directoryPath, e);
            throw new RuntimeException("Failed to add file to directory", e);
        }
    }

    public String uploadZippedDirectory(String directoryPath, String zipFileName) {
        log.info("Uploading zipped directory: {} as {}", directoryPath, zipFileName);

        try {
            return zipService.uploadZippedDirectory(directoryPath, zipFileName);
        } catch (Exception e) {
            log.error("Failed to upload zipped directory: {} as {}", directoryPath, zipFileName, e);
            throw new RuntimeException("Failed to upload zipped directory", e);
        }
    }

    public InputStream downloadZippedDirectory(String directoryPath) {
        log.info("Downloading zipped directory: {}", directoryPath);

        try {
            return zipService.createZipFromDirectory(directoryPath);
        } catch (Exception e) {
            log.error("Failed to download zipped directory: {}", directoryPath, e);
            throw new RuntimeException("Failed to download zipped directory", e);
        }
    }

    public void deleteDirectory(String directoryPath) {
        log.info("Deleting directory: {}", directoryPath);

        try {
            directoryManager.deleteDirectory(directoryPath);
            log.info("Successfully deleted directory: {}", directoryPath);
        } catch (Exception e) {
            log.error("Failed to delete directory: {}", directoryPath, e);
            throw new RuntimeException("Failed to delete directory", e);
        }
    }

    public String uploadSingleFile(MultipartFile file, String folder) {
        log.info("Uploading single file: {}", file.getOriginalFilename());

        try {
            String key = generateFileKey(file.getOriginalFilename(), folder);

            return fileStorage.uploadFile(
                    file.getInputStream(),
                    key,
                    file.getContentType(),
                    file.getSize()
            );

        } catch (Exception e) {
            log.error("Failed to upload single file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public InputStream downloadFile(String key) {
        log.info("Downloading file with key: {}", key);
        return fileStorage.downloadFile(key);
    }

    private String generateDirectoryPath(String baseDirectoryName) {
        String timestamp = LocalDateTime.now().format(FOLDER_FORMAT);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String cleanName = cleanFileName(baseDirectoryName);

        return String.format("projects/%s/%s-%s", timestamp, uniqueId, cleanName);
    }

    private String generateFileKey(String originalFilename, String folder) {
        String timestamp = LocalDateTime.now().format(FOLDER_FORMAT);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String filename = cleanFileName(originalFilename);

        StringBuilder keyBuilder = new StringBuilder();

        if (folder != null && !folder.trim().isEmpty()) {
            keyBuilder.append(folder).append("/");
        }

        keyBuilder.append(timestamp)
                .append("/")
                .append(uniqueId)
                .append("-")
                .append(filename);

        return keyBuilder.toString();
    }

    private String cleanFileName(String filename) {
        if (filename == null) {
            return "unknown-file";
        }

        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String ensureDirectoryPath(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return "";
        }

        String normalizedPath = directoryPath.trim().replace("\\", "/");

        if (!normalizedPath.endsWith("/")) {
            normalizedPath += "/";
        }

        return normalizedPath;
    }

    // ========================================
    // NOUVEAUX WORKFLOW : LOCAL → S3 → CLEANUP
    // ========================================

    /**
     * Crée un job de traitement avec fichiers vides sur le disque local
     * Les fichiers seront remplis par un processus batch externe (cp)
     *
     * @param projectName nom du projet
     * @param fileNames noms des fichiers à créer (vides)
     * @return les informations du job créé
     */
    public FileWorkflowManager.FileJob createFileJobWithEmptyFiles(String projectName, Set<String> fileNames) {
        log.info("Creating file job with empty files for project: {} with {} files", projectName, fileNames.size());

        try {
            FileWorkflowManager.FileJob job = workflowManager.createFileJob(projectName, fileNames);
            log.info("Successfully created file job: {}", job.jobId());
            return job;

        } catch (Exception e) {
            log.error("Failed to create file job for project: {}", projectName, e);
            throw new RuntimeException("Failed to create file job", e);
        }
    }

    /**
     * Vérifie si les fichiers d'un job ont été remplis par le batch externe
     *
     * @param jobId l'identifiant du job
     * @return true si tous les fichiers ne sont plus vides
     */
    public boolean areJobFilesReady(String jobId) {
        log.info("Checking if job files are ready: {}", jobId);
        return workflowManager.areFilesReady(jobId);
    }

    /**
     * Upload les fichiers d'un job vers S3 après vérification qu'ils sont prêts
     *
     * @param jobId l'identifiant du job
     * @return la liste des clés S3 des fichiers uploadés
     */
    public List<String> uploadJobFilesToS3(String jobId) {
        log.info("Uploading job files to S3: {}", jobId);

        try {
            List<String> uploadedKeys = workflowManager.uploadFilesToS3(jobId);
            log.info("Successfully uploaded {} files to S3 for job: {}", uploadedKeys.size(), jobId);
            return uploadedKeys;

        } catch (Exception e) {
            log.error("Failed to upload job files to S3: {}", jobId, e);
            throw new RuntimeException("Failed to upload job files to S3", e);
        }
    }

    /**
     * Nettoie les fichiers locaux après un upload réussi vers S3
     *
     * @param jobId l'identifiant du job
     */
    public void cleanupJobLocalFiles(String jobId) {
        log.info("Cleaning up local files for job: {}", jobId);

        try {
            workflowManager.cleanupLocalFiles(jobId);
            log.info("Successfully cleaned up local files for job: {}", jobId);

        } catch (Exception e) {
            log.error("Failed to cleanup local files for job: {}", jobId, e);
            throw new RuntimeException("Failed to cleanup local files", e);
        }
    }

    /**
     * Exécute le workflow complet : vérification → upload S3 → nettoyage
     *
     * @param jobId l'identifiant du job
     * @return la liste des clés S3 des fichiers uploadés
     */
    public List<String> processCompleteWorkflow(String jobId) {
        log.info("Processing complete workflow for job: {}", jobId);

        try {
            List<String> uploadedKeys = workflowManager.processCompleteWorkflow(jobId);
            log.info("Successfully completed workflow for job: {} with {} files uploaded", jobId, uploadedKeys.size());
            return uploadedKeys;

        } catch (Exception e) {
            log.error("Failed to process complete workflow for job: {}", jobId, e);
            throw new RuntimeException("Failed to process complete workflow", e);
        }
    }

    /**
     * Obtient les informations d'un job
     *
     * @param jobId l'identifiant du job
     * @return les informations du job
     */
    public FileWorkflowManager.FileJob getJobInfo(String jobId) {
        return workflowManager.getJobInfo(jobId);
    }

    /**
     * Supprime un job et nettoie toutes ses ressources
     *
     * @param jobId l'identifiant du job
     */
    public void deleteJob(String jobId) {
        log.info("Deleting job: {}", jobId);

        try {
            workflowManager.deleteJob(jobId);
            log.info("Successfully deleted job: {}", jobId);

        } catch (Exception e) {
            log.error("Failed to delete job: {}", jobId, e);
            throw new RuntimeException("Failed to delete job", e);
        }
    }

    /**
     * Liste tous les jobs actifs
     *
     * @return la liste des jobs actifs
     */
    public List<FileWorkflowManager.FileJob> listActiveJobs() {
        return workflowManager.listActiveJobs();
    }

    /**
     * Obtient le répertoire de travail temporaire configuré
     *
     * @return le chemin du répertoire temporaire
     */
    public String getTempWorkingDirectory() {
        return localFileManager.getTempWorkingDirectory().toString();
    }
}