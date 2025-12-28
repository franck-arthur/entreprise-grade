package com.enterprise.app.domain.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface LocalFileManager {

    /**
     * Crée un répertoire local avec des fichiers vides
     * @param directoryPath le chemin du répertoire à créer
     * @param fileNames les noms des fichiers vides à créer
     * @return le path absolu du répertoire créé
     */
    Path createDirectoryWithEmptyFiles(String directoryPath, Set<String> fileNames);

    /**
     * Crée un fichier vide dans un répertoire existant
     * @param directoryPath le chemin du répertoire
     * @param fileName le nom du fichier à créer
     * @return le path absolu du fichier créé
     */
    Path createEmptyFile(String directoryPath, String fileName);

    /**
     * Vérifie si un répertoire existe
     * @param directoryPath le chemin du répertoire
     * @return true si le répertoire existe
     */
    boolean directoryExists(String directoryPath);

    /**
     * Vérifie si un fichier existe
     * @param filePath le chemin du fichier
     * @return true si le fichier existe
     */
    boolean fileExists(String filePath);

    /**
     * Liste tous les fichiers dans un répertoire
     * @param directoryPath le chemin du répertoire
     * @return la liste des chemins de fichiers
     */
    List<Path> listFiles(String directoryPath);

    /**
     * Supprime un répertoire et tout son contenu
     * @param directoryPath le chemin du répertoire à supprimer
     */
    void deleteDirectory(String directoryPath);

    /**
     * Supprime un fichier
     * @param filePath le chemin du fichier à supprimer
     */
    void deleteFile(String filePath);

    /**
     * Obtient la taille d'un fichier
     * @param filePath le chemin du fichier
     * @return la taille en bytes
     */
    long getFileSize(String filePath);

    /**
     * Vérifie si un fichier est vide
     * @param filePath le chemin du fichier
     * @return true si le fichier est vide
     */
    boolean isFileEmpty(String filePath);

    /**
     * Obtient le répertoire racine temporaire pour les opérations
     * @return le path du répertoire racine
     */
    Path getTempWorkingDirectory();
}