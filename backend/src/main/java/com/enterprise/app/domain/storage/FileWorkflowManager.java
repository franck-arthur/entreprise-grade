package com.enterprise.app.domain.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface FileWorkflowManager {

    /**
     * Représente un job de traitement de fichiers avec workflow complet
     */
    record FileJob(
        String jobId,
        String projectName,
        Path localDirectory,
        Set<String> fileNames,
        FileJobStatus status
    ) {}

    enum FileJobStatus {
        CREATED,           // Répertoire et fichiers vides créés
        FILES_READY,       // Fichiers remplis par le batch
        UPLOADED_TO_S3,    // Fichiers uploadés sur S3
        CLEANED_UP,        // Fichiers locaux supprimés
        FAILED
    }

    /**
     * Crée un nouveau job avec répertoire local et fichiers vides
     * @param projectName le nom du projet
     * @param fileNames les noms des fichiers à créer (vides)
     * @return le job créé avec les informations du répertoire local
     */
    FileJob createFileJob(String projectName, Set<String> fileNames);

    /**
     * Vérifie si les fichiers ont été remplis par le batch externe
     * @param jobId l'ID du job
     * @return true si tous les fichiers ne sont plus vides
     */
    boolean areFilesReady(String jobId);

    /**
     * Upload tous les fichiers du job vers S3
     * @param jobId l'ID du job
     * @return la liste des clés S3 des fichiers uploadés
     */
    List<String> uploadFilesToS3(String jobId);

    /**
     * Nettoie les fichiers locaux après upload réussi
     * @param jobId l'ID du job
     */
    void cleanupLocalFiles(String jobId);

    /**
     * Workflow complet : vérifie que les fichiers sont prêts, upload vers S3, nettoie
     * @param jobId l'ID du job
     * @return la liste des clés S3 des fichiers uploadés
     */
    List<String> processCompleteWorkflow(String jobId);

    /**
     * Obtient les informations d'un job
     * @param jobId l'ID du job
     * @return les informations du job
     */
    FileJob getJobInfo(String jobId);

    /**
     * Supprime un job et nettoie toutes ses ressources
     * @param jobId l'ID du job
     */
    void deleteJob(String jobId);

    /**
     * Liste tous les jobs actifs
     * @return la liste des jobs actifs
     */
    List<FileJob> listActiveJobs();
}