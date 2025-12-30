package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.domain.model.csvexport.ExportExecution;
import com.enterprise.app.domain.model.csvexport.StatutExecution;
import com.enterprise.app.domain.repository.csvexport.ExportExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinalizeExportTrackingTasklet - Finalisation du tracking d'export")
class FinalizeExportTrackingTaskletTest {

    @Mock
    private ExportExecutionRepository exportExecutionRepository;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    @Mock
    private StepContext stepContext;

    @Mock
    private StepExecution stepExecution;

    @Mock
    private JobExecution jobExecution;

    @Mock
    private ExecutionContext executionContext;

    private FinalizeExportTrackingTasklet tasklet;

    @BeforeEach
    void setUp() {
        tasklet = new FinalizeExportTrackingTasklet(exportExecutionRepository);
    }

    @Test
    @DisplayName("Devrait finaliser un export terminé avec succès")
    void shouldFinalizeSuccessfulExport() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        Long trackingId = 123L;
        Integer totalLinesWritten = 1000;
        String outputFile = "/tmp/CSV_1_20240101_20240107.csv";

        ExportExecution export = ExportExecution.builder()
                .id(trackingId)
                .nomCsv(nomCsv)
                .statut(StatutExecution.EN_COURS)
                .build();

        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(executionContext.getLong("export.trackingId", -1L)).thenReturn(trackingId);
        when(executionContext.getInt("csv.totalLinesWritten", 0)).thenReturn(totalLinesWritten);
        when(executionContext.getString("csv.outputFile")).thenReturn(outputFile);
        when(exportExecutionRepository.findById(trackingId)).thenReturn(Optional.of(export));

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);

        ArgumentCaptor<ExportExecution> captor = ArgumentCaptor.forClass(ExportExecution.class);
        verify(exportExecutionRepository).save(captor.capture());

        ExportExecution capturedExport = captor.getValue();
        assertThat(capturedExport.getStatut()).isEqualTo(StatutExecution.TERMINE);
        assertThat(capturedExport.getNbLignesExportees()).isEqualTo(totalLinesWritten);
        assertThat(capturedExport.getCheminFichier()).isEqualTo(outputFile);
        assertThat(capturedExport.getDateFin()).isNotNull();
    }

    @Test
    @DisplayName("Devrait finaliser un export échoué")
    void shouldFinalizeFailedExport() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        Long trackingId = 123L;

        ExportExecution export = ExportExecution.builder()
                .id(trackingId)
                .nomCsv(nomCsv)
                .statut(StatutExecution.EN_COURS)
                .build();

        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(executionContext.getLong("export.trackingId", -1L)).thenReturn(trackingId);
        when(exportExecutionRepository.findById(trackingId)).thenReturn(Optional.of(export));

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);

        ArgumentCaptor<ExportExecution> captor = ArgumentCaptor.forClass(ExportExecution.class);
        verify(exportExecutionRepository).save(captor.capture());

        ExportExecution capturedExport = captor.getValue();
        assertThat(capturedExport.getStatut()).isEqualTo(StatutExecution.ECHEC);
        assertThat(capturedExport.getMessageErreur()).contains("Job terminé avec le statut: FAILED");
        assertThat(capturedExport.getDateFin()).isNotNull();
    }

    @Test
    @DisplayName("Devrait gérer l'absence d'ID de tracking")
    void shouldHandleMissingTrackingId() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getLong("export.trackingId", -1L)).thenReturn(-1L);

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        // Aucune sauvegarde ne doit avoir lieu
        verify(exportExecutionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Devrait gérer l'export non trouvé")
    void shouldHandleExportNotFound() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        Long trackingId = 999L;

        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(executionContext.getLong("export.trackingId", -1L)).thenReturn(trackingId);
        when(exportExecutionRepository.findById(trackingId)).thenReturn(Optional.empty());

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        // Aucune sauvegarde ne doit avoir lieu
        verify(exportExecutionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Devrait gérer l'absence de métriques d'export")
    void shouldHandleMissingExportMetrics() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        Long trackingId = 123L;

        ExportExecution export = ExportExecution.builder()
                .id(trackingId)
                .nomCsv(nomCsv)
                .statut(StatutExecution.EN_COURS)
                .build();

        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(executionContext.getLong("export.trackingId", -1L)).thenReturn(trackingId);
        when(executionContext.getInt("csv.totalLinesWritten", 0)).thenReturn(0);
        when(executionContext.getString("csv.outputFile")).thenReturn(null);
        when(exportExecutionRepository.findById(trackingId)).thenReturn(Optional.of(export));

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);

        ArgumentCaptor<ExportExecution> captor = ArgumentCaptor.forClass(ExportExecution.class);
        verify(exportExecutionRepository).save(captor.capture());

        ExportExecution capturedExport = captor.getValue();
        assertThat(capturedExport.getStatut()).isEqualTo(StatutExecution.TERMINE);
        assertThat(capturedExport.getNbLignesExportees()).isEqualTo(0);
        assertThat(capturedExport.getCheminFichier()).isNull();
    }

    @Test
    @DisplayName("Devrait gérer les erreurs lors de la récupération des métriques")
    void shouldHandleMetricsRetrievalErrors() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        Long trackingId = 123L;

        ExportExecution export = ExportExecution.builder()
                .id(trackingId)
                .nomCsv(nomCsv)
                .statut(StatutExecution.EN_COURS)
                .build();

        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(executionContext.getLong("export.trackingId", -1L)).thenReturn(trackingId);
        when(executionContext.getInt("csv.totalLinesWritten", 0))
                .thenThrow(new RuntimeException("Erreur contexte"));
        when(exportExecutionRepository.findById(trackingId)).thenReturn(Optional.of(export));

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);

        ArgumentCaptor<ExportExecution> captor = ArgumentCaptor.forClass(ExportExecution.class);
        verify(exportExecutionRepository).save(captor.capture());

        ExportExecution capturedExport = captor.getValue();
        assertThat(capturedExport.getStatut()).isEqualTo(StatutExecution.TERMINE);
        assertThat(capturedExport.getNbLignesExportees()).isNull();
    }

    @Test
    @DisplayName("Devrait calculer la durée d'exécution automatiquement")
    void shouldCalculateExecutionDurationAutomatically() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        Long trackingId = 123L;

        ExportExecution export = ExportExecution.builder()
                .id(trackingId)
                .nomCsv(nomCsv)
                .statut(StatutExecution.EN_COURS)
                .build();

        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(executionContext.getLong("export.trackingId", -1L)).thenReturn(trackingId);
        when(executionContext.getInt("csv.totalLinesWritten", 0)).thenReturn(500);
        when(exportExecutionRepository.findById(trackingId)).thenReturn(Optional.of(export));

        // When
        tasklet.execute(stepContribution, chunkContext);

        // Then
        ArgumentCaptor<ExportExecution> captor = ArgumentCaptor.forClass(ExportExecution.class);
        verify(exportExecutionRepository).save(captor.capture());

        ExportExecution capturedExport = captor.getValue();
        assertThat(capturedExport.getDateFin()).isNotNull();
        // La durée sera calculée automatiquement par la méthode markAsCompleted
    }

    private void setPrivateField(Object object, String fieldName, Object value) {
        try {
            var field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de définir le champ " + fieldName, e);
        }
    }
}