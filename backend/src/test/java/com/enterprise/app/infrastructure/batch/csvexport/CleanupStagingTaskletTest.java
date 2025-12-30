package com.enterprise.app.infrastructure.batch.csvexport;

import com.enterprise.app.domain.repository.csvexport.ExcelDefinitionStagingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CleanupStagingTasklet - Nettoyage des données de staging")
class CleanupStagingTaskletTest {

    @Mock
    private ExcelDefinitionStagingRepository stagingRepository;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    @Mock
    private StepContext stepContext;

    @Mock
    private StepExecution stepExecution;

    @Mock
    private ExecutionContext executionContext;

    @InjectMocks
    private CleanupStagingTasklet cleanupStagingTasklet;

    @Test
    @DisplayName("Devrait nettoyer les anciens enregistrements avec succès")
    void shouldCleanupOldRecordsSuccessfully() throws Exception {
        // Given
        long countBefore = 100L;
        int deletedOld = 25;
        long countAfter = 75L;

        when(stagingRepository.count()).thenReturn(countBefore).thenReturn(countAfter);
        when(stagingRepository.deleteOlderThan(any(LocalDateTime.class))).thenReturn(deletedOld);
        when(stepContribution.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);

        // When
        RepeatStatus result = cleanupStagingTasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(stagingRepository).deleteOlderThan(any(LocalDateTime.class));
        verify(executionContext).putLong("staging.recordsDeletedOld", deletedOld);
        verify(executionContext).putLong("staging.recordsRemaining", countAfter);
    }

    @Test
    @DisplayName("Devrait gérer le cas où aucun ancien enregistrement n'est supprimé")
    void shouldHandleNoOldRecordsToDelete() throws Exception {
        // Given
        long countBefore = 50L;
        int deletedOld = 0;
        long countAfter = 50L;

        when(stagingRepository.count()).thenReturn(countBefore).thenReturn(countAfter);
        when(stagingRepository.deleteOlderThan(any(LocalDateTime.class))).thenReturn(deletedOld);
        when(stepContribution.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);

        // When
        RepeatStatus result = cleanupStagingTasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(stagingRepository).deleteOlderThan(any(LocalDateTime.class));
        verify(executionContext).putLong("staging.recordsDeletedOld", 0);
        verify(executionContext).putLong("staging.recordsRemaining", 50L);
    }

    @Test
    @DisplayName("Devrait gérer une table de staging vide")
    void shouldHandleEmptyStagingTable() throws Exception {
        // Given
        long countBefore = 0L;
        int deletedOld = 0;
        long countAfter = 0L;

        when(stagingRepository.count()).thenReturn(countBefore).thenReturn(countAfter);
        when(stagingRepository.deleteOlderThan(any(LocalDateTime.class))).thenReturn(deletedOld);
        when(stepContribution.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getExecutionContext()).thenReturn(executionContext);

        // When
        RepeatStatus result = cleanupStagingTasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(stagingRepository).deleteOlderThan(any(LocalDateTime.class));
        verify(executionContext).putLong("staging.recordsDeletedOld", 0);
        verify(executionContext).putLong("staging.recordsRemaining", 0L);
    }

    @Test
    @DisplayName("Devrait gérer une erreur lors du comptage des enregistrements")
    void shouldHandleCountError() {
        // Given
        when(stagingRepository.count()).thenThrow(new RuntimeException("Erreur de base de données"));

        // When & Then
        assertThatThrownBy(() -> cleanupStagingTasklet.execute(stepContribution, chunkContext))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erreur de base de données");
    }

    @Test
    @DisplayName("Devrait gérer une erreur lors de la suppression")
    void shouldHandleDeleteError() {
        // Given
        long countBefore = 100L;
        when(stagingRepository.count()).thenReturn(countBefore);
        when(stagingRepository.deleteOlderThan(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Erreur lors de la suppression"));

        // When & Then
        assertThatThrownBy(() -> cleanupStagingTasklet.execute(stepContribution, chunkContext))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erreur lors de la suppression");
    }
}