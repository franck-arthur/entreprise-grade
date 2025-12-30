package com.enterprise.app.application.service.csvexport;

import com.enterprise.app.domain.model.csvexport.CsvColumnDefinition;
import com.enterprise.app.domain.repository.csvexport.CsvColumnDefinitionRepository;
import com.enterprise.app.domain.repository.csvexport.ExcelDefinitionStagingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service pour gérer l'import des fichiers Excel de définition CSV.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final JobLauncher jobLauncher;
    private final Job excelImportJob;
    private final CsvColumnDefinitionRepository columnDefinitionRepository;
    private final ExcelDefinitionStagingRepository stagingRepository;

    /**
     * Importe un fichier Excel de définitions CSV.
     *
     * @param excelFilePath Chemin vers le fichier Excel
     * @return L'ID de l'exécution du job
     */
    public Long importExcel(String excelFilePath) {
        log.info("Lancement de l'import Excel: {}", excelFilePath);

        try {
            // Construire les paramètres du job
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("excelFilePath", excelFilePath)
                    .addLong("timestamp", System.currentTimeMillis()) // Pour assurer l'unicité
                    .toJobParameters();

            // Lancer le job
            var jobExecution = jobLauncher.run(excelImportJob, jobParameters);
            Long jobExecutionId = jobExecution.getId();

            log.info("Import Excel lancé avec succès: job ID = {}", jobExecutionId);
            return jobExecutionId;

        } catch (Exception e) {
            log.error("Erreur lors du lancement de l'import Excel: {}", excelFilePath, e);
            throw new IllegalStateException("Erreur import Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Retourne les définitions de colonnes pour un CSV donné.
     */
    public List<CsvColumnDefinition> getColumnDefinitions(String nomCsv) {
        return columnDefinitionRepository.findByNomCsvAndActifTrueOrderByOrdreColonneAsc(nomCsv);
    }

    /**
     * Retourne toutes les définitions de colonnes (actives et inactives).
     */
    public List<CsvColumnDefinition> getAllColumnDefinitions(String nomCsv) {
        return columnDefinitionRepository.findByNomCsvOrderByOrdreColonneAsc(nomCsv);
    }

    /**
     * Vérifie les colonnes sans mapping technique.
     */
    public List<CsvColumnDefinition> getColumnsWithoutMapping(String nomCsv) {
        return columnDefinitionRepository.findColumnsWithoutMapping(nomCsv);
    }

    /**
     * Retourne le nombre de colonnes actives pour un CSV.
     */
    public long getActiveColumnCount(String nomCsv) {
        return columnDefinitionRepository.countByNomCsvAndActifTrue(nomCsv);
    }

    /**
     * Retourne les CSV distincts présents en staging.
     */
    public List<String> getAvailableCsvs() {
        return stagingRepository.findDistinctNomCsv();
    }

    /**
     * Valide qu'un CSV a toutes ses colonnes avec mapping technique.
     */
    public boolean validateCsvMapping(String nomCsv) {
        List<CsvColumnDefinition> columnsWithoutMapping = getColumnsWithoutMapping(nomCsv);

        if (!columnsWithoutMapping.isEmpty()) {
            log.warn("CSV {} : {} colonnes sans mapping technique", nomCsv, columnsWithoutMapping.size());
            for (CsvColumnDefinition col : columnsWithoutMapping) {
                log.warn("  - Colonne {} ({}): {}", col.getOrdreColonne(), col.getCodeVariable(), col.getLibelle());
            }
            return false;
        }

        log.info("CSV {} : toutes les colonnes ont un mapping technique", nomCsv);
        return true;
    }

    /**
     * Nettoie les données de staging anciennes.
     */
    public int cleanupOldStagingData(int daysOld) {
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(daysOld);
        int deletedCount = stagingRepository.deleteOlderThan(cutoffDate);
        log.info("Suppression de {} enregistrements de staging plus anciens que {} jours", deletedCount, daysOld);
        return deletedCount;
    }
}