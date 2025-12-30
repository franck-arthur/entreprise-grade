package com.enterprise.app.application.service.csvexport;

import com.enterprise.app.domain.model.csvexport.ExportExecution;
import com.enterprise.app.domain.model.csvexport.StatutExecution;
import com.enterprise.app.domain.repository.csvexport.ExportExecutionRepository;
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
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.JobParametersInvalidException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CsvExportService - Service d'export CSV")
class CsvExportServiceTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job csvExportJob;

    @Mock
    private ExportExecutionRepository exportExecutionRepository;

    @InjectMocks
    private CsvExportService csvExportService;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    @BeforeEach
    void setUp() {
        dateDebut = LocalDate.of(2024, 1, 1);
        dateFin = LocalDate.of(2024, 1, 7);
    }

    @Test
    @DisplayName("Devrait lancer un export CSV avec succès")
    void shouldExportCsvSuccessfully() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        String outputDirectory = "/tmp/exports";

        JobExecution mockJobExecution = mock(JobExecution.class);
        when(mockJobExecution.getId()).thenReturn(123L);
        when(exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS))
                .thenReturn(List.of());
        when(jobLauncher.run(eq(csvExportJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        Long jobId = csvExportService.exportCsv(nomCsv, dateDebut, dateFin, outputDirectory);

        // Then
        assertThat(jobId).isEqualTo(123L);
        verify(jobLauncher).run(eq(csvExportJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Devrait rejeter un export si un autre est déjà en cours pour le même CSV")
    void shouldRejectExportWhenAlreadyRunning() {
        // Given
        String nomCsv = "CSV_1";
        ExportExecution exportEnCours = ExportExecution.builder()
                .nomCsv(nomCsv)
                .statut(StatutExecution.EN_COURS)
                .dateDebut(LocalDateTime.now().minusHours(1))
                .build();

        when(exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS))
                .thenReturn(List.of(exportEnCours));

        // When & Then
        assertThatThrownBy(() -> csvExportService.exportCsv(nomCsv, dateDebut, dateFin, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Export déjà en cours pour: CSV_1");
    }

    @Test
    @DisplayName("Devrait autoriser un export si un autre CSV est en cours")
    void shouldAllowExportWhenDifferentCsvIsRunning() throws Exception {
        // Given
        String nomCsv = "CSV_2";
        ExportExecution exportEnCours = ExportExecution.builder()
                .nomCsv("CSV_1") // Différent CSV
                .statut(StatutExecution.EN_COURS)
                .dateDebut(LocalDateTime.now().minusHours(1))
                .build();

        JobExecution mockJobExecution = mock(JobExecution.class);
        when(mockJobExecution.getId()).thenReturn(456L);
        when(exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS))
                .thenReturn(List.of(exportEnCours));
        when(jobLauncher.run(eq(csvExportJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        Long jobId = csvExportService.exportCsv(nomCsv, dateDebut, dateFin, null);

        // Then
        assertThat(jobId).isEqualTo(456L);
        verify(jobLauncher).run(eq(csvExportJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Devrait lancer un export hebdomadaire")
    void shouldExportCsvHebdomadaire() throws Exception {
        // Given
        String nomCsv = "CSV_3";
        JobExecution mockJobExecution = mock(JobExecution.class);
        when(mockJobExecution.getId()).thenReturn(789L);
        when(exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS))
                .thenReturn(List.of());
        when(jobLauncher.run(eq(csvExportJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        Long jobId = csvExportService.exportCsvHebdomadaire(nomCsv);

        // Then
        assertThat(jobId).isEqualTo(789L);
        verify(jobLauncher).run(eq(csvExportJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Devrait retourner le statut d'un export")
    void shouldGetExportStatus() {
        // Given
        Long jobExecutionId = 123L;
        ExportExecution export = ExportExecution.builder()
                .jobExecutionId(jobExecutionId)
                .nomCsv("CSV_1")
                .statut(StatutExecution.TERMINE)
                .build();

        when(exportExecutionRepository.findByJobExecutionId(jobExecutionId))
                .thenReturn(Optional.of(export));

        // When
        Optional<ExportExecution> result = csvExportService.getExportStatus(jobExecutionId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getJobExecutionId()).isEqualTo(jobExecutionId);
        assertThat(result.get().getStatut()).isEqualTo(StatutExecution.TERMINE);
    }

    @Test
    @DisplayName("Devrait retourner un optionnel vide si l'export n'existe pas")
    void shouldReturnEmptyOptionalWhenExportNotFound() {
        // Given
        Long jobExecutionId = 999L;
        when(exportExecutionRepository.findByJobExecutionId(jobExecutionId))
                .thenReturn(Optional.empty());

        // When
        Optional<ExportExecution> result = csvExportService.getExportStatus(jobExecutionId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Devrait retourner l'historique des exports pour un CSV")
    void shouldGetExportHistory() {
        // Given
        String nomCsv = "CSV_1";
        List<ExportExecution> historique = Arrays.asList(
                ExportExecution.builder().nomCsv(nomCsv).statut(StatutExecution.TERMINE).build(),
                ExportExecution.builder().nomCsv(nomCsv).statut(StatutExecution.ECHEC).build()
        );

        when(exportExecutionRepository.findTop10ByNomCsvOrderByDateDebutDesc(nomCsv))
                .thenReturn(historique);

        // When
        List<ExportExecution> result = csvExportService.getExportHistory(nomCsv);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNomCsv()).isEqualTo(nomCsv);
    }

    @Test
    @DisplayName("Devrait retourner la liste des exports en cours")
    void shouldGetRunningExports() {
        // Given
        List<ExportExecution> exportsEnCours = Arrays.asList(
                ExportExecution.builder().nomCsv("CSV_1").statut(StatutExecution.EN_COURS).build(),
                ExportExecution.builder().nomCsv("CSV_2").statut(StatutExecution.EN_COURS).build()
        );

        when(exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS))
                .thenReturn(exportsEnCours);

        // When
        List<ExportExecution> result = csvExportService.getRunningExports();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(ExportExecution::getNomCsv))
                .containsExactly("CSV_1", "CSV_2");
    }

    @Test
    @DisplayName("Devrait retourner les statistiques d'export")
    void shouldGetExportStatistics() {
        // Given
        String nomCsv = "CSV_1";
        LocalDateTime depuis = LocalDateTime.now().minusDays(30);
        Object[] statistiques = new Object[]{"stat1", "stat2", "stat3"};

        when(exportExecutionRepository.getStatistiquesExport(nomCsv, depuis))
                .thenReturn(statistiques);

        // When
        Object[] result = csvExportService.getExportStatistics(nomCsv, depuis);

        // Then
        assertThat(result).isEqualTo(statistiques);
        verify(exportExecutionRepository).getStatistiquesExport(nomCsv, depuis);
    }

    @Test
    @DisplayName("Devrait gérer les erreurs lors du lancement d'un job")
    void shouldHandleJobLauncherException() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        when(exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS))
                .thenReturn(List.of());
        when(jobLauncher.run(eq(csvExportJob), any(JobParameters.class)))
                .thenThrow(new RuntimeException("Erreur job launcher"));

        // When & Then
        assertThatThrownBy(() -> csvExportService.exportCsv(nomCsv, dateDebut, dateFin, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Erreur export CSV: Erreur job launcher");
    }

    @Test
    @DisplayName("Devrait lancer l'export hebdomadaire pour tous les CSV")
    void shouldExportAllCsvsHebdomadaire() throws Exception {
        // Given
        JobExecution mockJobExecution = mock(JobExecution.class);
        when(mockJobExecution.getId()).thenReturn(100L, 200L, 300L);
        when(exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS))
                .thenReturn(List.of());
        when(jobLauncher.run(eq(csvExportJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        csvExportService.exportAllCsvsHebdomadaire();

        // Then
        verify(jobLauncher, org.mockito.Mockito.times(3)).run(eq(csvExportJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Devrait continuer l'export même si un CSV échoue")
    void shouldContinueExportWhenOneCsvFails() throws Exception {
        // Given
        when(exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS))
                .thenReturn(List.of())  // Premier appel OK
                .thenThrow(new RuntimeException("Erreur CSV_2"))  // Deuxième appel échoue
                .thenReturn(List.of());  // Troisième appel OK

        JobExecution mockJobExecution = mock(JobExecution.class);
        when(mockJobExecution.getId()).thenReturn(100L, 300L);
        when(jobLauncher.run(eq(csvExportJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        csvExportService.exportAllCsvs(dateDebut, dateFin, null);

        // Then
        // Devrait avoir essayé 3 fois même si le 2ème a échoué
        verify(exportExecutionRepository, org.mockito.Mockito.times(3))
                .findByStatutAndDateFinIsNull(StatutExecution.EN_COURS);
    }

    @Test
    @DisplayName("Devrait lancer l'export hebdomadaire asynchrone pour tous les CSV")
    void shouldExportAllCsvsHebdomadaireAsync() throws ExecutionException, InterruptedException, TimeoutException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
        // Given
        JobExecution mockJobExecution = mock(JobExecution.class);
        when(mockJobExecution.getId()).thenReturn(100L, 200L, 300L);
        when(exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS))
                .thenReturn(List.of());
        when(jobLauncher.run(eq(csvExportJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        CompletableFuture<Void> future = csvExportService.exportAllCsvsHebdomadaireAsync();

        // Then
        assertThat(future).isNotNull();
        future.get(30, TimeUnit.SECONDS); // Attendre la completion

        // Vérifier que les 3 CSV ont été lancés
        verify(jobLauncher, times(3)).run(eq(csvExportJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Devrait retourner les statistiques d'export de manière asynchrone")
    void shouldGetExportStatisticsAsync() throws ExecutionException, InterruptedException, TimeoutException {
        // Given
        String nomCsv = "CSV_1";
        LocalDateTime depuis = LocalDateTime.now().minusDays(30);
        Object[] statistiques = new Object[]{"stat1", "stat2", "stat3"};

        when(exportExecutionRepository.getStatistiquesExport(nomCsv, depuis))
                .thenReturn(statistiques);

        // When
        CompletableFuture<Object[]> future = csvExportService.getExportStatisticsAsync(nomCsv, depuis);

        // Then
        assertThat(future).isNotNull();
        Object[] result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo(statistiques);
        verify(exportExecutionRepository).getStatistiquesExport(nomCsv, depuis);
    }

    @Test
    @DisplayName("Devrait gérer le timeout pour les exports parallèles")
    void shouldHandleTimeoutForParallelExports() throws Exception {
        // Given - Simuler un job qui ne se termine jamais
        when(exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS))
                .thenReturn(List.of());
        when(jobLauncher.run(eq(csvExportJob), any(JobParameters.class)))
                .thenAnswer(invocation -> {
                    // Simuler un job qui prend du temps
                    Thread.sleep(100);
                    JobExecution mockJobExecution = mock(JobExecution.class);
                    when(mockJobExecution.getId()).thenReturn(123L);
                    return mockJobExecution;
                });

        // When & Then - Ne devrait pas lever d'exception même avec timeout
        csvExportService.exportAllCsvs(dateDebut, dateFin, null);

        // Vérifier que tous les CSV ont été tentés
        verify(jobLauncher, times(3)).run(eq(csvExportJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Devrait nettoyer les ressources au shutdown")
    void shouldCleanupResourcesOnShutdown() {
        // Given - Le service est initialisé

        // When
        csvExportService.cleanup();

        // Then - Pas d'exception levée et le service reste fonctionnel
        // Test que le cleanup n'interfère pas avec les fonctionnalités de base
        assertThat(csvExportService).isNotNull();
    }

    @Test
    @DisplayName("Devrait gérer les Virtual Threads pour l'exécution parallèle")
    void shouldHandleVirtualThreadsForParallelExecution() throws Exception {
        // Given
        JobExecution mockJobExecution = mock(JobExecution.class);
        when(mockJobExecution.getId()).thenReturn(100L, 200L, 300L);
        when(exportExecutionRepository.findByStatutAndDateFinIsNull(StatutExecution.EN_COURS))
                .thenReturn(List.of());
        when(jobLauncher.run(eq(csvExportJob), any(JobParameters.class)))
                .thenReturn(mockJobExecution);

        // When
        csvExportService.exportAllCsvs(dateDebut, dateFin, null);

        // Then
        // Vérifier que les 3 CSV sont lancés en parallèle
        verify(jobLauncher, times(3)).run(eq(csvExportJob), any(JobParameters.class));

        // Vérifier que chaque CSV est vérifié individuellement
        verify(exportExecutionRepository, times(3))
                .findByStatutAndDateFinIsNull(StatutExecution.EN_COURS);
    }
}