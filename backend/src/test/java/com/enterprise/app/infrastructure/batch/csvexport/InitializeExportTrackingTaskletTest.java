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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InitializeExportTrackingTasklet - Initialisation du tracking d'export")
class InitializeExportTrackingTaskletTest {

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

    private InitializeExportTrackingTasklet tasklet;

    @BeforeEach
    void setUp() {
        tasklet = new InitializeExportTrackingTasklet(exportExecutionRepository);
    }

    @Test
    @DisplayName("Devrait initialiser le tracking avec succès")
    void shouldInitializeTrackingSuccessfully() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        Long jobExecutionId = 123L;
        Long trackingId = 456L;

        ExportExecution savedExport = ExportExecution.builder()
                .id(trackingId)
                .nomCsv(nomCsv)
                .jobExecutionId(jobExecutionId)
                .statut(StatutExecution.EN_COURS)
                .build();

        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getId()).thenReturn(jobExecutionId);
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
        when(exportExecutionRepository.save(any(ExportExecution.class))).thenReturn(savedExport);
        when(stepContribution.getStepExecution()).thenReturn(stepExecution);

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);

        ArgumentCaptor<ExportExecution> captor = ArgumentCaptor.forClass(ExportExecution.class);
        verify(exportExecutionRepository).save(captor.capture());

        ExportExecution capturedExport = captor.getValue();
        assertThat(capturedExport.getNomCsv()).isEqualTo(nomCsv);
        assertThat(capturedExport.getJobExecutionId()).isEqualTo(jobExecutionId);
        assertThat(capturedExport.getStatut()).isEqualTo(StatutExecution.EN_COURS);

        verify(executionContext).putLong("export.trackingId", trackingId);
    }

    @Test
    @DisplayName("Devrait gérer correctement les paramètres du job")
    void shouldHandleJobParametersCorrectly() throws Exception {
        // Given
        String nomCsv = "CSV_2";
        Long jobExecutionId = 789L;
        Long trackingId = 101L;

        ExportExecution savedExport = ExportExecution.builder()
                .id(trackingId)
                .nomCsv(nomCsv)
                .jobExecutionId(jobExecutionId)
                .statut(StatutExecution.EN_COURS)
                .build();

        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getId()).thenReturn(jobExecutionId);
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
        when(exportExecutionRepository.save(any(ExportExecution.class))).thenReturn(savedExport);
        when(stepContribution.getStepExecution()).thenReturn(stepExecution);

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);

        ArgumentCaptor<ExportExecution> captor = ArgumentCaptor.forClass(ExportExecution.class);
        verify(exportExecutionRepository).save(captor.capture());

        ExportExecution capturedExport = captor.getValue();
        assertThat(capturedExport.getNomCsv()).isEqualTo(nomCsv);
        assertThat(capturedExport.getJobExecutionId()).isEqualTo(jobExecutionId);
    }

    @Test
    @DisplayName("Devrait créer un export avec le statut EN_COURS")
    void shouldCreateExportWithInProgressStatus() throws Exception {
        // Given
        String nomCsv = "CSV_3";
        Long jobExecutionId = 999L;
        Long trackingId = 555L;

        ExportExecution savedExport = ExportExecution.builder()
                .id(trackingId)
                .build();

        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getId()).thenReturn(jobExecutionId);
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
        when(exportExecutionRepository.save(any(ExportExecution.class))).thenReturn(savedExport);
        when(stepContribution.getStepExecution()).thenReturn(stepExecution);

        // When
        tasklet.execute(stepContribution, chunkContext);

        // Then
        ArgumentCaptor<ExportExecution> captor = ArgumentCaptor.forClass(ExportExecution.class);
        verify(exportExecutionRepository).save(captor.capture());

        ExportExecution capturedExport = captor.getValue();
        assertThat(capturedExport.getStatut()).isEqualTo(StatutExecution.EN_COURS);
        assertThat(capturedExport.getDateDebut()).isNotNull();
    }

    @Test
    @DisplayName("Devrait stocker l'ID de tracking dans le contexte")
    void shouldStoreTrackingIdInContext() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        Long jobExecutionId = 123L;
        Long trackingId = 777L;

        ExportExecution savedExport = ExportExecution.builder()
                .id(trackingId)
                .build();

        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getId()).thenReturn(jobExecutionId);
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
        when(exportExecutionRepository.save(any(ExportExecution.class))).thenReturn(savedExport);
        when(stepContribution.getStepExecution()).thenReturn(stepExecution);

        // When
        tasklet.execute(stepContribution, chunkContext);

        // Then
        verify(executionContext).putLong("export.trackingId", trackingId);
    }

    @Test
    @DisplayName("Devrait utiliser la méthode factory create de ExportExecution")
    void shouldUseFactoryCreateMethod() throws Exception {
        // Given
        String nomCsv = "CSV_1";
        Long jobExecutionId = 123L;

        ExportExecution mockSavedExport = ExportExecution.builder().id(1L).build();

        setPrivateField(tasklet, "nomCsv", nomCsv);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getId()).thenReturn(jobExecutionId);
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);
        when(exportExecutionRepository.save(any(ExportExecution.class))).thenReturn(mockSavedExport);
        when(stepContribution.getStepExecution()).thenReturn(stepExecution);

        // When
        tasklet.execute(stepContribution, chunkContext);

        // Then
        ArgumentCaptor<ExportExecution> captor = ArgumentCaptor.forClass(ExportExecution.class);
        verify(exportExecutionRepository).save(captor.capture());

        ExportExecution capturedExport = captor.getValue();
        assertThat(capturedExport.getNomCsv()).isEqualTo(nomCsv);
        assertThat(capturedExport.getJobExecutionId()).isEqualTo(jobExecutionId);
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