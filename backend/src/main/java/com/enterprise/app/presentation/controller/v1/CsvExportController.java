package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.service.csvexport.CsvExportService;
import com.enterprise.app.application.service.csvexport.ExcelImportService;
import com.enterprise.app.domain.model.csvexport.CsvColumnDefinition;
import com.enterprise.app.domain.model.csvexport.ExportExecution;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.micrometer.observation.annotation.Observed;

/**
 * Contrôleur REST pour la gestion des exports CSV.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/csv-export")
@RequiredArgsConstructor
@Tag(name = "CSV Export", description = "Gestion des exports CSV et import Excel")
@PreAuthorize("hasRole('ADMIN')")
public class CsvExportController {

    private final CsvExportService csvExportService;
    private final ExcelImportService excelImportService;

    // ================================
    // GESTION DES EXPORTS CSV
    // ================================

    @PostMapping("/export")
    @Operation(summary = "Lancer un export CSV",
               description = "Lance l'export d'un CSV pour une période donnée")
    @Observed(
        name = "csv.export.api",
        contextualName = "csv-export-api-endpoint",
        lowCardinalityKeyValues = {"controller", "csv-export", "operation", "export"}
    )
    public ResponseEntity<Map<String, Object>> exportCsv(
            @Parameter(description = "Nom du CSV (CSV_1, CSV_2, CSV_3)", example = "CSV_1")
            @RequestParam String nomCsv,

            @Parameter(description = "Date de début", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,

            @Parameter(description = "Date de fin", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,

            @Parameter(description = "Répertoire de sortie (optionnel)")
            @RequestParam(required = false) String outputDirectory) {

        log.info("Demande d'export CSV: {} pour la période {} - {}", nomCsv, dateDebut, dateFin);

        try {
            Long jobExecutionId = csvExportService.exportCsv(nomCsv, dateDebut, dateFin, outputDirectory);

            return ResponseEntity.ok(Map.of(
                    "status", "started",
                    "message", "Export lancé avec succès",
                    "jobExecutionId", jobExecutionId,
                    "nomCsv", nomCsv
            ));

        } catch (Exception e) {
            log.error("Erreur lors du lancement de l'export CSV", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Erreur lors du lancement de l'export: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/export/all")
    @Operation(summary = "Lancer l'export de tous les CSV",
               description = "Lance l'export de tous les CSV (CSV_1, CSV_2, CSV_3)")
    public ResponseEntity<Map<String, Object>> exportAllCsvs(
            @Parameter(description = "Date de début", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,

            @Parameter(description = "Date de fin", example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,

            @Parameter(description = "Répertoire de sortie (optionnel)")
            @RequestParam(required = false) String outputDirectory) {

        log.info("Demande d'export de tous les CSV pour la période {} - {}", dateDebut, dateFin);

        try {
            csvExportService.exportAllCsvs(dateDebut, dateFin, outputDirectory);

            return ResponseEntity.ok(Map.of(
                    "status", "started",
                    "message", "Export de tous les CSV lancé avec succès",
                    "periode", Map.of(
                            "debut", dateDebut.toString(),
                            "fin", dateFin.toString()
                    )
            ));

        } catch (Exception e) {
            log.error("Erreur lors du lancement de l'export de tous les CSV", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Erreur lors du lancement de l'export: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/export/weekly")
    @Operation(summary = "Lancer l'export hebdomadaire",
               description = "Lance l'export hebdomadaire (7 derniers jours) pour tous les CSV")
    public ResponseEntity<Map<String, Object>> exportWeekly() {
        log.info("Demande d'export hebdomadaire");

        try {
            csvExportService.exportAllCsvsHebdomadaire();

            return ResponseEntity.ok(Map.of(
                    "status", "started",
                    "message", "Export hebdomadaire lancé avec succès"
            ));

        } catch (Exception e) {
            log.error("Erreur lors du lancement de l'export hebdomadaire", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Erreur lors du lancement de l'export: " + e.getMessage()
            ));
        }
    }

    // ================================
    // MONITORING ET STATUT
    // ================================

    @GetMapping("/status/{jobExecutionId}")
    @Operation(summary = "Statut d'un export",
               description = "Récupère le statut d'un export en cours ou terminé")
    public ResponseEntity<ExportExecution> getExportStatus(
            @Parameter(description = "ID de l'exécution du job", example = "123")
            @PathVariable Long jobExecutionId) {

        Optional<ExportExecution> export = csvExportService.getExportStatus(jobExecutionId);

        return export.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/history/{nomCsv}")
    @Operation(summary = "Historique des exports",
               description = "Récupère l'historique des 10 derniers exports pour un CSV donné")
    public ResponseEntity<List<ExportExecution>> getExportHistory(
            @Parameter(description = "Nom du CSV", example = "CSV_1")
            @PathVariable String nomCsv) {

        List<ExportExecution> history = csvExportService.getExportHistory(nomCsv);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/running")
    @Operation(summary = "Exports en cours",
               description = "Liste tous les exports actuellement en cours d'exécution")
    public ResponseEntity<List<ExportExecution>> getRunningExports() {
        List<ExportExecution> runningExports = csvExportService.getRunningExports();
        return ResponseEntity.ok(runningExports);
    }

    @GetMapping("/statistics/{nomCsv}")
    @Operation(summary = "Statistiques d'export",
               description = "Récupère les statistiques d'export pour un CSV donné")
    public ResponseEntity<Map<String, Object>> getExportStatistics(
            @Parameter(description = "Nom du CSV", example = "CSV_1")
            @PathVariable String nomCsv,

            @Parameter(description = "Date de début pour les statistiques", example = "2024-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime depuis) {

        if (depuis == null) {
            depuis = LocalDateTime.now().minusMonths(1); // Dernier mois par défaut
        }

        Object[] stats = csvExportService.getExportStatistics(nomCsv, depuis);

        return ResponseEntity.ok(Map.of(
                "nomCsv", nomCsv,
                "periode", Map.of(
                        "depuis", depuis.toString(),
                        "jusquA", LocalDateTime.now().toString()
                ),
                "statistiques", stats
        ));
    }

    // ================================
    // IMPORT EXCEL
    // ================================

    @PostMapping("/import-excel")
    @Operation(summary = "Importer un fichier Excel",
               description = "Importe un fichier Excel contenant les définitions des colonnes CSV")
    public ResponseEntity<Map<String, Object>> importExcel(
            @Parameter(description = "Fichier Excel à importer")
            @RequestParam("file") MultipartFile file) {

        log.info("Demande d'import Excel: {}", file.getOriginalFilename());

        try {
            // Sauvegarder temporairement le fichier
            File tempFile = File.createTempFile("excel-import-", ".xlsx");
            file.transferTo(tempFile);

            // Lancer l'import
            Long jobExecutionId = excelImportService.importExcel(tempFile.getAbsolutePath());

            return ResponseEntity.ok(Map.of(
                    "status", "started",
                    "message", "Import Excel lancé avec succès",
                    "jobExecutionId", jobExecutionId,
                    "fileName", file.getOriginalFilename()
            ));

        } catch (IOException e) {
            log.error("Erreur lors de la sauvegarde du fichier Excel", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Erreur lors de la sauvegarde du fichier: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Erreur lors du lancement de l'import Excel", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Erreur lors du lancement de l'import: " + e.getMessage()
            ));
        }
    }

    // ================================
    // GESTION DES DÉFINITIONS
    // ================================

    @GetMapping("/definitions/{nomCsv}")
    @Operation(summary = "Définitions de colonnes",
               description = "Récupère les définitions de colonnes pour un CSV donné")
    public ResponseEntity<List<CsvColumnDefinition>> getColumnDefinitions(
            @Parameter(description = "Nom du CSV", example = "CSV_1")
            @PathVariable String nomCsv) {

        List<CsvColumnDefinition> definitions = excelImportService.getColumnDefinitions(nomCsv);
        return ResponseEntity.ok(definitions);
    }

    @GetMapping("/definitions/{nomCsv}/unmapped")
    @Operation(summary = "Colonnes sans mapping",
               description = "Récupère les colonnes qui n'ont pas de mapping technique")
    public ResponseEntity<List<CsvColumnDefinition>> getColumnsWithoutMapping(
            @Parameter(description = "Nom du CSV", example = "CSV_1")
            @PathVariable String nomCsv) {

        List<CsvColumnDefinition> unmappedColumns = excelImportService.getColumnsWithoutMapping(nomCsv);
        return ResponseEntity.ok(unmappedColumns);
    }

    @GetMapping("/definitions/{nomCsv}/validate")
    @Operation(summary = "Valider le mapping",
               description = "Valide que toutes les colonnes ont un mapping technique")
    public ResponseEntity<Map<String, Object>> validateCsvMapping(
            @Parameter(description = "Nom du CSV", example = "CSV_1")
            @PathVariable String nomCsv) {

        boolean isValid = excelImportService.validateCsvMapping(nomCsv);
        long totalColumns = excelImportService.getActiveColumnCount(nomCsv);
        List<CsvColumnDefinition> unmappedColumns = excelImportService.getColumnsWithoutMapping(nomCsv);

        return ResponseEntity.ok(Map.of(
                "nomCsv", nomCsv,
                "valide", isValid,
                "totalColonnes", totalColumns,
                "colonnesSansMapping", unmappedColumns.size(),
                "details", unmappedColumns
        ));
    }
}