package com.enterprise.app.domain.model.csvexport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExportExecution - Tests des méthodes métier")
class ExportExecutionMethodsTest {

    @Test
    @DisplayName("Devrait marquer un export comme démarré")
    void shouldMarkExportAsStarted() {
        // Given
        ExportExecution export = new ExportExecution();

        // When
        export.markAsStarted();

        // Then
        assertThat(export.getStatut()).isEqualTo(StatutExecution.EN_COURS);
        assertThat(export.getDateDebut()).isNotNull();
        assertThat(export.getDateDebut()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Devrait marquer un export comme terminé avec succès")
    void shouldMarkExportAsCompleted() {
        // Given
        ExportExecution export = ExportExecution.builder()
                .dateDebut(LocalDateTime.now().minusMinutes(5))
                .build();

        Integer nbLignes = 1500;
        String cheminFichier = "/exports/csv_1_20240101.csv";

        // When
        export.markAsCompleted(nbLignes, cheminFichier);

        // Then
        assertThat(export.getStatut()).isEqualTo(StatutExecution.TERMINE);
        assertThat(export.getDateFin()).isNotNull();
        assertThat(export.getNbLignesExportees()).isEqualTo(nbLignes);
        assertThat(export.getCheminFichier()).isEqualTo(cheminFichier);
        assertThat(export.getDureeSecondes()).isNotNull();
        assertThat(export.getDureeSecondes().compareTo(BigDecimal.ZERO)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Devrait marquer un export comme échoué")
    void shouldMarkExportAsFailed() {
        // Given
        ExportExecution export = ExportExecution.builder()
                .dateDebut(LocalDateTime.now().minusMinutes(2))
                .build();

        String messageErreur = "Connexion à la base de données impossible";

        // When
        export.markAsFailed(messageErreur);

        // Then
        assertThat(export.getStatut()).isEqualTo(StatutExecution.ECHEC);
        assertThat(export.getDateFin()).isNotNull();
        assertThat(export.getMessageErreur()).isEqualTo(messageErreur);
        assertThat(export.getDureeSecondes()).isNotNull();
        assertThat(export.getDureeSecondes().compareTo(BigDecimal.ZERO)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Devrait détecter qu'un export est terminé (succès)")
    void shouldDetectCompletedSuccessfulExport() {
        // Given
        ExportExecution export = ExportExecution.builder()
                .statut(StatutExecution.TERMINE)
                .build();

        // When & Then
        assertThat(export.isCompleted()).isTrue();
        assertThat(export.isSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Devrait détecter qu'un export est terminé (échec)")
    void shouldDetectCompletedFailedExport() {
        // Given
        ExportExecution export = ExportExecution.builder()
                .statut(StatutExecution.ECHEC)
                .build();

        // When & Then
        assertThat(export.isCompleted()).isTrue();
        assertThat(export.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Devrait détecter qu'un export n'est pas terminé")
    void shouldDetectRunningExport() {
        // Given
        ExportExecution export = ExportExecution.builder()
                .statut(StatutExecution.EN_COURS)
                .build();

        // When & Then
        assertThat(export.isCompleted()).isFalse();
        assertThat(export.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Devrait détecter qu'un export annulé est terminé mais pas réussi")
    void shouldDetectCancelledExport() {
        // Given
        ExportExecution export = ExportExecution.builder()
                .statut(StatutExecution.ANNULE)
                .build();

        // When & Then
        assertThat(export.isCompleted()).isFalse(); // ANNULE n'est pas considéré comme terminé
        assertThat(export.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Devrait créer un nouvel export avec la méthode statique")
    void shouldCreateNewExportWithStaticMethod() {
        // Given
        String nomCsv = "CSV_TEST";
        Long jobExecutionId = 42L;

        // When
        ExportExecution export = ExportExecution.create(nomCsv, jobExecutionId);

        // Then
        assertThat(export.getNomCsv()).isEqualTo(nomCsv);
        assertThat(export.getJobExecutionId()).isEqualTo(jobExecutionId);
        assertThat(export.getStatut()).isEqualTo(StatutExecution.EN_COURS);
        assertThat(export.getDateDebut()).isNotNull();
        assertThat(export.getDateDebut()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Devrait calculer la durée correctement")
    void shouldCalculateDurationCorrectly() {
        // Given
        LocalDateTime dateDebut = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        ExportExecution export = ExportExecution.builder()
                .dateDebut(dateDebut)
                .build();

        // When - Simuler un délai de 30 secondes
        LocalDateTime dateFin = dateDebut.plusSeconds(30);
        export.setDateFin(dateFin);
        export.markAsCompleted(100, "/test/file.csv");

        // Then
        assertThat(export.getDureeSecondes()).isEqualTo(new BigDecimal("30"));
    }

    @Test
    @DisplayName("Devrait ne pas calculer de durée si dateDebut est null")
    void shouldNotCalculateDurationWhenDateDebutIsNull() {
        // Given
        ExportExecution export = new ExportExecution();
        export.setDateFin(LocalDateTime.now());

        // When
        export.markAsFailed("Test error");

        // Then
        assertThat(export.getDureeSecondes()).isNull();
    }

    @Test
    @DisplayName("Devrait ne pas calculer de durée si dateFin est null")
    void shouldNotCalculateDurationWhenDateFinIsNull() {
        // Given
        ExportExecution export = ExportExecution.builder()
                .dateDebut(LocalDateTime.now())
                .build();

        // When - Ne pas définir dateFin, juste le statut
        export.setStatut(StatutExecution.EN_COURS);

        // Then
        assertThat(export.getDureeSecondes()).isNull();
    }

    @Test
    @DisplayName("Devrait gérer les cas limites de durée")
    void shouldHandleEdgeCasesForDuration() {
        // Given
        LocalDateTime dateDebut = LocalDateTime.now();
        ExportExecution export = ExportExecution.builder()
                .dateDebut(dateDebut)
                .build();

        // When - Export instantané (même milliseconde)
        export.markAsCompleted(0, "/empty/file.csv");

        // Then
        assertThat(export.getDureeSecondes()).isNotNull();
        assertThat(export.getDureeSecondes().compareTo(BigDecimal.ZERO)).isGreaterThanOrEqualTo(0);
    }
}