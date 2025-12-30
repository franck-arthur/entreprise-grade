package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.service.csvexport.CsvExportService;
import com.enterprise.app.application.service.csvexport.ExcelImportService;
import com.enterprise.app.domain.model.csvexport.CsvColumnDefinition;
import com.enterprise.app.domain.model.csvexport.ExportExecution;
import com.enterprise.app.domain.model.csvexport.StatutExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CsvExportController.class)
@ContextConfiguration(classes = TestWebMvcConfiguration.class)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.datasource.url=",
    "spring.jpa.database=default"
})
@DisplayName("CsvExportController - Contrôleur REST pour les exports CSV")
class CsvExportControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockBean
    private CsvExportService csvExportService;

    @MockBean
    private ExcelImportService excelImportService;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    @BeforeEach
    void setUp() {
        dateDebut = LocalDate.of(2024, 1, 1);
        dateFin = LocalDate.of(2024, 1, 7);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait lancer un export CSV avec succès")
    void shouldExportCsvSuccessfully() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        Long jobExecutionId = 123L;
        when(csvExportService.exportCsv(eq(nomCsv), eq(dateDebut), eq(dateFin), any()))
                .thenReturn(jobExecutionId);

        // When & Then
        mockMvc.perform(post("/api/v1/csv-export/export")
                        .param("nomCsv", nomCsv)
                        .param("dateDebut", dateDebut.toString())
                        .param("dateFin", dateFin.toString())
                        .param("outputDirectory", "/tmp/exports")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("started"))
                .andExpect(jsonPath("$.jobExecutionId").value(123))
                .andExpect(jsonPath("$.nomCsv").value(nomCsv));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait retourner une erreur quand l'export échoue")
    void shouldReturnErrorWhenExportFails() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        when(csvExportService.exportCsv(eq(nomCsv), eq(dateDebut), eq(dateFin), any()))
                .thenThrow(new IllegalStateException("Export déjà en cours"));

        // When & Then
        mockMvc.perform(post("/api/v1/csv-export/export")
                        .param("nomCsv", nomCsv)
                        .param("dateDebut", dateDebut.toString())
                        .param("dateFin", dateFin.toString())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Erreur lors du lancement de l'export: Export déjà en cours"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait lancer l'export de tous les CSV")
    void shouldExportAllCsvs() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/csv-export/export/all")
                        .param("dateDebut", dateDebut.toString())
                        .param("dateFin", dateFin.toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("started"))
                .andExpect(jsonPath("$.message").value("Export de tous les CSV lancé avec succès"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait lancer l'export hebdomadaire")
    void shouldExportWeekly() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/csv-export/export/weekly")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("started"))
                .andExpect(jsonPath("$.message").value("Export hebdomadaire lancé avec succès"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait retourner le statut d'un export")
    void shouldGetExportStatus() throws Exception {
        // Given
        Long jobExecutionId = 123L;
        ExportExecution export = ExportExecution.builder()
                .jobExecutionId(jobExecutionId)
                .nomCsv("CSV_1")
                .statut(StatutExecution.TERMINE)
                .dateDebut(LocalDateTime.now())
                .build();

        when(csvExportService.getExportStatus(jobExecutionId))
                .thenReturn(Optional.of(export));

        // When & Then
        mockMvc.perform(get("/api/v1/csv-export/status/{jobExecutionId}", jobExecutionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobExecutionId").value(123))
                .andExpect(jsonPath("$.nomCsv").value("CSV_1"))
                .andExpect(jsonPath("$.statut").value("TERMINE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait retourner 404 quand l'export n'existe pas")
    void shouldReturn404WhenExportNotFound() throws Exception {
        // Given
        Long jobExecutionId = 999L;
        when(csvExportService.getExportStatus(jobExecutionId))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/csv-export/status/{jobExecutionId}", jobExecutionId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait retourner l'historique des exports")
    void shouldGetExportHistory() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        List<ExportExecution> history = Arrays.asList(
                ExportExecution.builder().nomCsv(nomCsv).statut(StatutExecution.TERMINE).build(),
                ExportExecution.builder().nomCsv(nomCsv).statut(StatutExecution.ECHEC).build()
        );

        when(csvExportService.getExportHistory(nomCsv)).thenReturn(history);

        // When & Then
        mockMvc.perform(get("/api/v1/csv-export/history/{nomCsv}", nomCsv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait retourner les exports en cours")
    void shouldGetRunningExports() throws Exception {
        // Given
        List<ExportExecution> runningExports = Arrays.asList(
                ExportExecution.builder().nomCsv("CSV_1").statut(StatutExecution.EN_COURS).build()
        );

        when(csvExportService.getRunningExports()).thenReturn(runningExports);

        // When & Then
        mockMvc.perform(get("/api/v1/csv-export/running"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait retourner les statistiques d'export")
    void shouldGetExportStatistics() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        Object[] stats = new Object[]{"stat1", "stat2", "stat3"};
        when(csvExportService.getExportStatistics(eq(nomCsv), any(LocalDateTime.class)))
                .thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/v1/csv-export/statistics/{nomCsv}", nomCsv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomCsv").value(nomCsv))
                .andExpect(jsonPath("$.statistiques").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait importer un fichier Excel")
    void shouldImportExcel() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-definitions.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "test content".getBytes()
        );

        Long jobExecutionId = 456L;
        when(excelImportService.importExcel(anyString())).thenReturn(jobExecutionId);

        // When & Then
        mockMvc.perform(multipart("/api/v1/csv-export/import-excel")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("started"))
                .andExpect(jsonPath("$.jobExecutionId").value(456))
                .andExpect(jsonPath("$.fileName").value("test-definitions.xlsx"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait retourner les définitions de colonnes")
    void shouldGetColumnDefinitions() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        List<CsvColumnDefinition> definitions = Arrays.asList(
                CsvColumnDefinition.builder().codeVariable("COL1").libelle("Colonne 1").build(),
                CsvColumnDefinition.builder().codeVariable("COL2").libelle("Colonne 2").build()
        );

        when(excelImportService.getColumnDefinitions(nomCsv)).thenReturn(definitions);

        // When & Then
        mockMvc.perform(get("/api/v1/csv-export/definitions/{nomCsv}", nomCsv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codeVariable").value("COL1"))
                .andExpect(jsonPath("$[1].codeVariable").value("COL2"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait retourner les colonnes sans mapping")
    void shouldGetColumnsWithoutMapping() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        List<CsvColumnDefinition> unmappedColumns = Arrays.asList(
                CsvColumnDefinition.builder().codeVariable("COL5").libelle("Sans mapping").build()
        );

        when(excelImportService.getColumnsWithoutMapping(nomCsv)).thenReturn(unmappedColumns);

        // When & Then
        mockMvc.perform(get("/api/v1/csv-export/definitions/{nomCsv}/unmapped", nomCsv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codeVariable").value("COL5"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devrait valider le mapping CSV")
    void shouldValidateCsvMapping() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        when(excelImportService.validateCsvMapping(nomCsv)).thenReturn(true);
        when(excelImportService.getActiveColumnCount(nomCsv)).thenReturn(10L);
        when(excelImportService.getColumnsWithoutMapping(nomCsv)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/csv-export/definitions/{nomCsv}/validate", nomCsv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomCsv").value(nomCsv))
                .andExpect(jsonPath("$.valide").value(true))
                .andExpect(jsonPath("$.totalColonnes").value(10))
                .andExpect(jsonPath("$.colonnesSansMapping").value(0));
    }

    @Test
    @DisplayName("Devrait rejeter les requêtes non authentifiées")
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(post("/api/v1/csv-export/export")
                        .param("nomCsv", "CSV_1")
                        .param("dateDebut", dateDebut.toString())
                        .param("dateFin", dateFin.toString())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Devrait rejeter les requêtes sans rôle ADMIN")
    void shouldRejectNonAdminRequests() throws Exception {
        mockMvc.perform(post("/api/v1/csv-export/export")
                        .param("nomCsv", "CSV_1")
                        .param("dateDebut", dateDebut.toString())
                        .param("dateFin", dateFin.toString())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}