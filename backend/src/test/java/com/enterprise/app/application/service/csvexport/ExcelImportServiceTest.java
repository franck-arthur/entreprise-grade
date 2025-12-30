package com.enterprise.app.application.service.csvexport;

import com.enterprise.app.domain.model.csvexport.CsvColumnDefinition;
import com.enterprise.app.domain.repository.csvexport.CsvColumnDefinitionRepository;
import com.enterprise.app.domain.repository.csvexport.ExcelDefinitionStagingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExcelImportService - Service d'import des fichiers Excel")
class ExcelImportServiceTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job excelImportJob;

    @Mock
    private CsvColumnDefinitionRepository columnDefinitionRepository;

    @Mock
    private ExcelDefinitionStagingRepository stagingRepository;

    @InjectMocks
    private ExcelImportService excelImportService;

    private String excelFilePath;
    private String nomCsv;

    @BeforeEach
    void setUp() {
        excelFilePath = "/tmp/test-definitions.xlsx";
        nomCsv = "CSV_1";
    }

    @Test
    @DisplayName("Devrait importer un fichier Excel avec succès")
    void shouldImportExcelSuccessfully() throws Exception {
        // Given
        JobExecution mockJobExecution = mock(JobExecution.class);
        when(mockJobExecution.getId()).thenReturn(123L);
        when(jobLauncher.run(eq(excelImportJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        Long jobId = excelImportService.importExcel(excelFilePath);

        // Then
        assertThat(jobId).isEqualTo(123L);
        verify(jobLauncher).run(eq(excelImportJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Devrait gérer les erreurs lors de l'import Excel")
    void shouldHandleExcelImportError() throws Exception {
        // Given
        when(jobLauncher.run(eq(excelImportJob), any(JobParameters.class)))
                .thenThrow(new RuntimeException("Erreur lors de l'import"));

        // When & Then
        assertThatThrownBy(() -> excelImportService.importExcel(excelFilePath))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Erreur import Excel: Erreur lors de l'import");
    }

    @Test
    @DisplayName("Devrait retourner les définitions de colonnes actives pour un CSV")
    void shouldGetActiveColumnDefinitions() {
        // Given
        List<CsvColumnDefinition> definitions = Arrays.asList(
                createColumnDefinition(1, "COL1", "Colonne 1", true),
                createColumnDefinition(2, "COL2", "Colonne 2", true)
        );

        when(columnDefinitionRepository.findByNomCsvAndActifTrueOrderByOrdreColonneAsc(nomCsv))
                .thenReturn(definitions);

        // When
        List<CsvColumnDefinition> result = excelImportService.getColumnDefinitions(nomCsv);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCodeVariable()).isEqualTo("COL1");
        assertThat(result.get(1).getCodeVariable()).isEqualTo("COL2");
        verify(columnDefinitionRepository).findByNomCsvAndActifTrueOrderByOrdreColonneAsc(nomCsv);
    }

    @Test
    @DisplayName("Devrait retourner toutes les définitions de colonnes (actives et inactives)")
    void shouldGetAllColumnDefinitions() {
        // Given
        List<CsvColumnDefinition> definitions = Arrays.asList(
                createColumnDefinition(1, "COL1", "Colonne 1", true),
                createColumnDefinition(2, "COL2", "Colonne 2", false),
                createColumnDefinition(3, "COL3", "Colonne 3", true)
        );

        when(columnDefinitionRepository.findByNomCsvOrderByOrdreColonneAsc(nomCsv))
                .thenReturn(definitions);

        // When
        List<CsvColumnDefinition> result = excelImportService.getAllColumnDefinitions(nomCsv);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getActif()).isTrue();
        assertThat(result.get(1).getActif()).isFalse();
        assertThat(result.get(2).getActif()).isTrue();
    }

    @Test
    @DisplayName("Devrait retourner les colonnes sans mapping technique")
    void shouldGetColumnsWithoutMapping() {
        // Given
        List<CsvColumnDefinition> columnsWithoutMapping = Arrays.asList(
                createColumnDefinition(5, "COL5", "Colonne sans mapping", true),
                createColumnDefinition(8, "COL8", "Autre colonne sans mapping", true)
        );

        when(columnDefinitionRepository.findColumnsWithoutMapping(nomCsv))
                .thenReturn(columnsWithoutMapping);

        // When
        List<CsvColumnDefinition> result = excelImportService.getColumnsWithoutMapping(nomCsv);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCodeVariable()).isEqualTo("COL5");
        assertThat(result.get(1).getCodeVariable()).isEqualTo("COL8");
    }

    @Test
    @DisplayName("Devrait compter les colonnes actives pour un CSV")
    void shouldCountActiveColumns() {
        // Given
        when(columnDefinitionRepository.countByNomCsvAndActifTrue(nomCsv))
                .thenReturn(15L);

        // When
        long count = excelImportService.getActiveColumnCount(nomCsv);

        // Then
        assertThat(count).isEqualTo(15L);
        verify(columnDefinitionRepository).countByNomCsvAndActifTrue(nomCsv);
    }

    @Test
    @DisplayName("Devrait retourner les CSV disponibles")
    void shouldGetAvailableCsvs() {
        // Given
        List<String> csvNames = Arrays.asList("CSV_1", "CSV_2", "CSV_3");
        when(stagingRepository.findDistinctNomCsv()).thenReturn(csvNames);

        // When
        List<String> result = excelImportService.getAvailableCsvs();

        // Then
        assertThat(result).containsExactly("CSV_1", "CSV_2", "CSV_3");
        verify(stagingRepository).findDistinctNomCsv();
    }

    @Test
    @DisplayName("Devrait valider qu'un CSV a toutes ses colonnes avec mapping")
    void shouldValidateCsvMappingWhenComplete() {
        // Given
        when(columnDefinitionRepository.findColumnsWithoutMapping(nomCsv))
                .thenReturn(List.of());

        // When
        boolean isValid = excelImportService.validateCsvMapping(nomCsv);

        // Then
        assertThat(isValid).isTrue();
        verify(columnDefinitionRepository).findColumnsWithoutMapping(nomCsv);
    }

    @Test
    @DisplayName("Devrait invalider un CSV avec des colonnes sans mapping")
    void shouldInvalidateCsvMappingWhenIncomplete() {
        // Given
        List<CsvColumnDefinition> columnsWithoutMapping = Arrays.asList(
                createColumnDefinition(5, "COL5", "Colonne sans mapping", true)
        );

        when(columnDefinitionRepository.findColumnsWithoutMapping(nomCsv))
                .thenReturn(columnsWithoutMapping);

        // When
        boolean isValid = excelImportService.validateCsvMapping(nomCsv);

        // Then
        assertThat(isValid).isFalse();
        verify(columnDefinitionRepository).findColumnsWithoutMapping(nomCsv);
    }

    @Test
    @DisplayName("Devrait nettoyer les données de staging anciennes")
    void shouldCleanupOldStagingData() {
        // Given
        int daysOld = 30;
        int expectedDeletedCount = 150;
        when(stagingRepository.deleteOlderThan(any(LocalDateTime.class)))
                .thenReturn(expectedDeletedCount);

        // When
        int deletedCount = excelImportService.cleanupOldStagingData(daysOld);

        // Then
        assertThat(deletedCount).isEqualTo(expectedDeletedCount);
        verify(stagingRepository).deleteOlderThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Devrait retourner zéro quand aucune donnée ancienne à nettoyer")
    void shouldReturnZeroWhenNoOldDataToCleanup() {
        // Given
        int daysOld = 7;
        when(stagingRepository.deleteOlderThan(any(LocalDateTime.class)))
                .thenReturn(0);

        // When
        int deletedCount = excelImportService.cleanupOldStagingData(daysOld);

        // Then
        assertThat(deletedCount).isZero();
        verify(stagingRepository).deleteOlderThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Devrait retourner une liste vide quand aucun CSV disponible")
    void shouldReturnEmptyListWhenNoCsvsAvailable() {
        // Given
        when(stagingRepository.findDistinctNomCsv()).thenReturn(List.of());

        // When
        List<String> result = excelImportService.getAvailableCsvs();

        // Then
        assertThat(result).isEmpty();
    }

    private CsvColumnDefinition createColumnDefinition(int ordre, String codeVariable, String libelle, boolean actif) {
        return CsvColumnDefinition.builder()
                .nomCsv(nomCsv)
                .ordreColonne(ordre)
                .codeVariable(codeVariable)
                .libelle(libelle)
                .actif(actif)
                .build();
    }
}