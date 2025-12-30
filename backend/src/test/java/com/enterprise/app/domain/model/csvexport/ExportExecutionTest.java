package com.enterprise.app.domain.model.csvexport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests pour ExportExecution")
class ExportExecutionTest {

    @Test
    @DisplayName("Doit créer une instance valide avec le builder")
    void builder_ShouldCreateValidInstance() {
        // Given & When
        ExportExecution execution = ExportExecution.builder()
                .nomCsv("test_export")
                .jobExecutionId(123L)
                .statut(StatutExecution.EN_COURS)
                .nbLignesExportees(100)
                .build();

        // Then
        assertThat(execution).isNotNull();
        assertThat(execution.getNomCsv()).isEqualTo("test_export");
        assertThat(execution.getJobExecutionId()).isEqualTo(123L);
        assertThat(execution.getStatut()).isEqualTo(StatutExecution.EN_COURS);
        assertThat(execution.getNbLignesExportees()).isEqualTo(100);
    }

    @Test
    @DisplayName("Doit créer un nouveau tracking d'export avec la méthode factory create")
    void create_ShouldCreateNewExportTracking() {
        // Given
        String nomCsv = "formation_export";
        Long jobExecutionId = 456L;
        LocalDateTime before = LocalDateTime.now();

        // When
        ExportExecution execution = ExportExecution.create(nomCsv, jobExecutionId);

        // Then
        assertThat(execution).isNotNull();
        assertThat(execution.getNomCsv()).isEqualTo(nomCsv);
        assertThat(execution.getJobExecutionId()).isEqualTo(jobExecutionId);
        assertThat(execution.getStatut()).isEqualTo(StatutExecution.EN_COURS);
        assertThat(execution.getDateDebut()).isAfterOrEqualTo(before);
        assertThat(execution.getDateDebut()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Doit marquer l'export comme démarré avec markAsStarted")
    void markAsStarted_ShouldSetStatusAndStartTime() {
        // Given
        ExportExecution execution = new ExportExecution();
        LocalDateTime before = LocalDateTime.now();

        // When
        execution.markAsStarted();

        // Then
        assertThat(execution.getStatut()).isEqualTo(StatutExecution.EN_COURS);
        assertThat(execution.getDateDebut()).isAfterOrEqualTo(before);
        assertThat(execution.getDateDebut()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Doit marquer l'export comme terminé avec succès avec markAsCompleted")
    void markAsCompleted_ShouldSetStatusAndCompletionData() {
        // Given
        ExportExecution execution = ExportExecution.builder()
                .dateDebut(LocalDateTime.now().minusMinutes(5))
                .build();
        Integer nbLignes = 250;
        String cheminFichier = "/exports/test.csv";

        // When
        execution.markAsCompleted(nbLignes, cheminFichier);

        // Then
        assertThat(execution.getStatut()).isEqualTo(StatutExecution.TERMINE);
        assertThat(execution.getNbLignesExportees()).isEqualTo(nbLignes);
        assertThat(execution.getCheminFichier()).isEqualTo(cheminFichier);
        assertThat(execution.getDateFin()).isNotNull();
        assertThat(execution.getDureeSecondes()).isNotNull();
        assertThat(execution.getDureeSecondes()).isPositive();
    }

    @Test
    @DisplayName("Doit marquer l'export comme échoué avec markAsFailed")
    void markAsFailed_ShouldSetStatusAndErrorMessage() {
        // Given
        ExportExecution execution = ExportExecution.builder()
                .dateDebut(LocalDateTime.now().minusMinutes(2))
                .build();
        String messageErreur = "Erreur de connexion à la base de données";

        // When
        execution.markAsFailed(messageErreur);

        // Then
        assertThat(execution.getStatut()).isEqualTo(StatutExecution.ECHEC);
        assertThat(execution.getMessageErreur()).isEqualTo(messageErreur);
        assertThat(execution.getDateFin()).isNotNull();
        assertThat(execution.getDureeSecondes()).isNotNull();
        assertThat(execution.getDureeSecondes()).isPositive();
    }

    @Test
    @DisplayName("Doit calculer correctement la durée d'exécution")
    void calculateDuration_ShouldComputeCorrectDuration() {
        // Given
        LocalDateTime debut = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2024, 1, 1, 10, 2, 30); // 2 minutes 30 secondes

        ExportExecution execution = ExportExecution.builder()
                .dateDebut(debut)
                .build();

        // When
        execution.setDateFin(fin);
        execution.markAsCompleted(100, "/test.csv"); // Cela déclenchera calculateDuration

        // Then
        assertThat(execution.getDureeSecondes()).isNotNull();
        assertThat(execution.getDureeSecondes()).isEqualByComparingTo(BigDecimal.valueOf(150.0)); // 2*60 + 30 = 150 secondes
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.5, 30.0, 65.0, 3661.0})
    @DisplayName("Doit formater correctement la durée pour l'affichage")
    void getFormattedDuration_ShouldFormatCorrectly(double seconds) {
        // Given
        ExportExecution execution = ExportExecution.builder()
                .dureeSecondes(BigDecimal.valueOf(seconds))
                .build();

        // When
        String formatted = execution.getFormattedDuration();

        // Then
        assertThat(formatted).isNotNull();
        assertThat(formatted).isNotBlank();

        if (seconds < 60) {
            assertThat(formatted).endsWith("s");
            assertThat(formatted).contains("1.5"); // Pour 1.5 secondes
        } else if (seconds < 3600) {
            assertThat(formatted).contains("m");
            assertThat(formatted).contains("s");
        } else {
            assertThat(formatted).contains("h");
            assertThat(formatted).contains("m");
            assertThat(formatted).contains("s");
        }
    }

    @Test
    @DisplayName("Doit retourner 'N/A' pour un formatage de durée nulle")
    void getFormattedDuration_ShouldReturnNAForNullDuration() {
        // Given
        ExportExecution execution = new ExportExecution();

        // When
        String formatted = execution.getFormattedDuration();

        // Then
        assertThat(formatted).isEqualTo("N/A");
    }

    @Test
    @DisplayName("Doit identifier correctement les exports terminés")
    void isCompleted_ShouldUseStatutExecutionLogic() {
        // Given
        ExportExecution executionEnCours = ExportExecution.builder()
                .statut(StatutExecution.EN_COURS)
                .build();
        ExportExecution executionTermine = ExportExecution.builder()
                .statut(StatutExecution.TERMINE)
                .build();
        ExportExecution executionEchec = ExportExecution.builder()
                .statut(StatutExecution.ECHEC)
                .build();

        // When & Then
        assertThat(executionEnCours.isCompleted()).isFalse();
        assertThat(executionTermine.isCompleted()).isTrue();
        assertThat(executionEchec.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Doit identifier correctement les exports réussis")
    void isSuccessful_ShouldDelegateToStatutExecution() {
        // Given
        ExportExecution executionTermine = ExportExecution.builder()
                .statut(StatutExecution.TERMINE)
                .build();
        ExportExecution executionEchec = ExportExecution.builder()
                .statut(StatutExecution.ECHEC)
                .build();

        // When & Then
        assertThat(executionTermine.isSuccessful()).isTrue();
        assertThat(executionEchec.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Doit identifier correctement les exports en échec")
    void isFailed_ShouldDelegateToStatutExecution() {
        // Given
        ExportExecution executionEchec = ExportExecution.builder()
                .statut(StatutExecution.ECHEC)
                .build();
        ExportExecution executionAnnule = ExportExecution.builder()
                .statut(StatutExecution.ANNULE)
                .build();
        ExportExecution executionTermine = ExportExecution.builder()
                .statut(StatutExecution.TERMINE)
                .build();

        // When & Then
        assertThat(executionEchec.isFailed()).isTrue();
        assertThat(executionAnnule.isFailed()).isTrue();
        assertThat(executionTermine.isFailed()).isFalse();
    }

    @Test
    @DisplayName("Doit retourner la bonne priorité pour l'affichage")
    void getPriority_ShouldReturnCorrectPriority() {
        // Given
        ExportExecution executionTermine = ExportExecution.builder()
                .statut(StatutExecution.TERMINE)
                .build();
        ExportExecution executionEchec = ExportExecution.builder()
                .statut(StatutExecution.ECHEC)
                .build();
        ExportExecution executionSansStatut = new ExportExecution();

        // When & Then
        assertThat(executionTermine.getPriority()).isEqualTo(StatutExecution.Priority.SUCCESS);
        assertThat(executionEchec.getPriority()).isEqualTo(StatutExecution.Priority.ERROR);
        assertThat(executionSansStatut.getPriority()).isEqualTo(StatutExecution.Priority.INFO);
    }

    @Test
    @DisplayName("Doit avoir l'égalité basée sur jobExecutionId, nomCsv et dateDebut")
    void equals_ShouldBeBasedOnCompositeKey() {
        // Given
        LocalDateTime date = LocalDateTime.now();
        ExportExecution execution1 = ExportExecution.builder()
                .jobExecutionId(123L)
                .nomCsv("test")
                .dateDebut(date)
                .nbLignesExportees(100) // Champ différent
                .build();

        ExportExecution execution2 = ExportExecution.builder()
                .jobExecutionId(123L)
                .nomCsv("test")
                .dateDebut(date)
                .nbLignesExportees(200) // Champ différent
                .build();

        ExportExecution execution3 = ExportExecution.builder()
                .jobExecutionId(456L) // jobExecutionId différent
                .nomCsv("test")
                .dateDebut(date)
                .build();

        // When & Then
        assertThat(execution1).isEqualTo(execution2);
        assertThat(execution1).isNotEqualTo(execution3);
        assertThat(execution1.hashCode()).isEqualTo(execution2.hashCode());
    }

    @Test
    @DisplayName("Doit utiliser AllArgsConstructor correctement")
    void allArgsConstructor_ShouldSetAllFields() {
        // Given
        Long id = 1L;
        Long jobExecutionId = 123L;
        String nomCsv = "test_csv";
        LocalDateTime dateDebut = LocalDateTime.now().minusHours(1);
        LocalDateTime dateFin = LocalDateTime.now();
        StatutExecution statut = StatutExecution.TERMINE;
        Integer nbLignesExportees = 500;
        String cheminFichier = "/exports/test.csv";
        String messageErreur = null;
        BigDecimal dureeSecondes = BigDecimal.valueOf(3600.0);

        // When
        ExportExecution execution = new ExportExecution(
                id, jobExecutionId, nomCsv, dateDebut, dateFin,
                statut, nbLignesExportees, cheminFichier, messageErreur, dureeSecondes
        );

        // Then
        assertThat(execution.getId()).isEqualTo(id);
        assertThat(execution.getJobExecutionId()).isEqualTo(jobExecutionId);
        assertThat(execution.getNomCsv()).isEqualTo(nomCsv);
        assertThat(execution.getDateDebut()).isEqualTo(dateDebut);
        assertThat(execution.getDateFin()).isEqualTo(dateFin);
        assertThat(execution.getStatut()).isEqualTo(statut);
        assertThat(execution.getNbLignesExportees()).isEqualTo(nbLignesExportees);
        assertThat(execution.getCheminFichier()).isEqualTo(cheminFichier);
        assertThat(execution.getMessageErreur()).isEqualTo(messageErreur);
        assertThat(execution.getDureeSecondes()).isEqualTo(dureeSecondes);
    }

    @Test
    @DisplayName("Doit permettre la modification des propriétés via les setters")
    void setters_ShouldModifyProperties() {
        // Given
        ExportExecution execution = new ExportExecution();
        String nouveauNom = "nouveau_export";
        Integer nouvelleLignes = 1000;
        StatutExecution nouveauStatut = StatutExecution.ECHEC;

        // When
        execution.setNomCsv(nouveauNom);
        execution.setNbLignesExportees(nouvelleLignes);
        execution.setStatut(nouveauStatut);

        // Then
        assertThat(execution.getNomCsv()).isEqualTo(nouveauNom);
        assertThat(execution.getNbLignesExportees()).isEqualTo(nouvelleLignes);
        assertThat(execution.getStatut()).isEqualTo(nouveauStatut);
    }

    @Test
    @DisplayName("Doit inclure tous les champs importants dans toString")
    void toString_ShouldIncludeImportantFields() {
        // Given
        ExportExecution execution = ExportExecution.builder()
                .nomCsv("test_export")
                .jobExecutionId(123L)
                .statut(StatutExecution.TERMINE)
                .build();

        // When
        String toStringResult = execution.toString();

        // Then
        assertThat(toStringResult).contains("nomCsv");
        assertThat(toStringResult).contains("jobExecutionId");
        assertThat(toStringResult).contains("statut");
    }

    @Test
    @DisplayName("Doit utiliser les nouvelles fonctionnalités Java 21 dans le formatage")
    void getFormattedDuration_ShouldUseJava21Features() {
        // Given - Test du String.formatted() de Java 15+
        ExportExecution execution = ExportExecution.builder()
                .dureeSecondes(BigDecimal.valueOf(45.5))
                .build();

        // When
        String formatted = execution.getFormattedDuration();

        // Then - Vérifie que le formatage fonctionne avec la nouvelle API
        assertThat(formatted).matches("\\d+\\.\\ds");
        assertThat(formatted).isEqualTo("45.5s");
    }
}