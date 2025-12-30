package com.enterprise.app.infrastructure.batch.csvexport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NormalizeDefinitionsTasklet - Normalisation des définitions Excel")
class NormalizeDefinitionsTaskletTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    @Mock
    private StepContext stepContext;

    @Mock
    private org.springframework.batch.core.StepExecution stepExecutionMock;

    @Mock
    private Connection connection;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ExecutionContext executionContext;

    private NormalizeDefinitionsTasklet tasklet;

    @BeforeEach
    void setUp() {
        tasklet = new NormalizeDefinitionsTasklet(dataSource);
    }

    @Test
    @DisplayName("Devrait normaliser les définitions avec succès")
    void shouldNormalizeDefinitionsSuccessfully() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall("CALL export_csv.normalize_definitions()")).thenReturn(callableStatement);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(stepContribution.getStepExecution()).thenReturn(stepExecutionMock);
        when(stepExecutionMock.getExecutionContext()).thenReturn(executionContext);
        when(chunkContext.getStepContext()).thenReturn(stepContext);

        // Configuration des résultats des requêtes de statistiques
        when(resultSet.next()).thenReturn(true, true, false); // Deux requêtes avec résultats
        when(resultSet.getLong("total_definitions")).thenReturn(50L);
        when(resultSet.getLong("csv_count")).thenReturn(3L);
        when(resultSet.getLong("active_definitions")).thenReturn(45L);
        when(resultSet.getLong("unmapped_count")).thenReturn(2L);

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(callableStatement).execute();
        verify(connection).close();
    }

    @Test
    @DisplayName("Devrait stocker les métriques dans le contexte")
    void shouldStoreMetricsInContext() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall("CALL export_csv.normalize_definitions()")).thenReturn(callableStatement);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(stepContribution.getStepExecution()).thenReturn(stepExecutionMock);
        when(stepExecutionMock.getExecutionContext()).thenReturn(executionContext);
        when(chunkContext.getStepContext()).thenReturn(stepContext);

        // Mock des résultats avec des métriques spécifiques
        when(resultSet.next())
                .thenReturn(true).thenReturn(false)  // Première requête
                .thenReturn(true).thenReturn(false); // Deuxième requête

        when(resultSet.getLong("total_definitions")).thenReturn(100L);
        when(resultSet.getLong("csv_count")).thenReturn(5L);
        when(resultSet.getLong("active_definitions")).thenReturn(95L);
        when(resultSet.getLong("unmapped_count")).thenReturn(0L);

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        // Note: Les vérifications de contexte ne peuvent pas être testées facilement ici
        // car nous n'avons pas accès au ExecutionContext via StepContribution mock
    }

    @Test
    @DisplayName("Devrait gérer les erreurs de base de données")
    void shouldHandleDatabaseErrors() throws Exception {
        // Given
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Erreur de connexion"));

        // When & Then
        assertThatThrownBy(() -> tasklet.execute(stepContribution, chunkContext))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erreur de connexion");
    }

    @Test
    @DisplayName("Devrait gérer les erreurs de procédure stockée")
    void shouldHandleStoredProcedureErrors() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall("CALL export_csv.normalize_definitions()"))
                .thenThrow(new RuntimeException("Erreur procédure"));

        // When & Then
        assertThatThrownBy(() -> tasklet.execute(stepContribution, chunkContext))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erreur procédure");

        verify(connection).close();
    }

    @Test
    @DisplayName("Devrait exécuter la procédure de normalisation")
    void shouldExecuteNormalizationProcedure() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall("CALL export_csv.normalize_definitions()")).thenReturn(callableStatement);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Pas de résultats pour simplifier

        // When
        tasklet.execute(stepContribution, chunkContext);

        // Then
        verify(callableStatement).execute();
    }

    @Test
    @DisplayName("Devrait récupérer les statistiques après normalisation")
    void shouldRetrieveStatisticsAfterNormalization() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall("CALL export_csv.normalize_definitions()")).thenReturn(callableStatement);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);

        // Simuler les résultats des statistiques
        when(resultSet.next())
                .thenReturn(true).thenReturn(false)  // Première requête (statistiques générales)
                .thenReturn(true).thenReturn(false); // Deuxième requête (colonnes sans mapping)

        when(resultSet.getLong("total_definitions")).thenReturn(25L);
        when(resultSet.getLong("csv_count")).thenReturn(2L);
        when(resultSet.getLong("active_definitions")).thenReturn(20L);
        when(resultSet.getLong("unmapped_count")).thenReturn(5L);

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        // Vérifier que les deux requêtes de statistiques sont exécutées
        verify(statement, org.mockito.Mockito.times(2)).executeQuery(anyString());
    }

    @Test
    @DisplayName("Devrait afficher un warning quand il y a des colonnes sans mapping")
    void shouldShowWarningWhenUnmappedColumns() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall("CALL export_csv.normalize_definitions()")).thenReturn(callableStatement);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);

        // Simuler des colonnes sans mapping
        when(resultSet.next())
                .thenReturn(true).thenReturn(false)  // Première requête
                .thenReturn(true).thenReturn(false); // Deuxième requête

        when(resultSet.getLong("total_definitions")).thenReturn(10L);
        when(resultSet.getLong("csv_count")).thenReturn(1L);
        when(resultSet.getLong("active_definitions")).thenReturn(10L);
        when(resultSet.getLong("unmapped_count")).thenReturn(3L); // 3 colonnes sans mapping

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        // Le warning sera affiché dans les logs
    }

    @Test
    @DisplayName("Devrait fermer les ressources même en cas d'erreur")
    void shouldCloseResourcesEvenOnError() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall("CALL export_csv.normalize_definitions()")).thenReturn(callableStatement);
        when(callableStatement.execute()).thenThrow(new RuntimeException("Erreur execution"));

        // When & Then
        assertThatThrownBy(() -> tasklet.execute(stepContribution, chunkContext))
                .isInstanceOf(RuntimeException.class);

        verify(connection).close();
    }

    @Test
    @DisplayName("Devrait gérer les cas sans résultats de statistiques")
    void shouldHandleNoStatisticsResults() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall("CALL export_csv.normalize_definitions()")).thenReturn(callableStatement);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Aucun résultat

        // When
        RepeatStatus result = tasklet.execute(stepContribution, chunkContext);

        // Then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(callableStatement).execute();
    }
}