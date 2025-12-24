package com.enterprise.app.testing.fixtures;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.ModaliteFormation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Fixtures centralisées pour les données de test Formation.
 * Fournit des instances prédéfinies et réutilisables pour les tests.
 */
public final class FormationFixtures {

    private FormationFixtures() {}

    // IDs constants pour les tests
    public static final UUID FORMATION_ID_1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    public static final UUID FORMATION_ID_2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");
    public static final UUID FORMATION_ID_3 = UUID.fromString("123e4567-e89b-12d3-a456-426614174003");

    // Libellés constants
    public static final String FORMATION_LIBELLE_JAVA = "Formation Java Avancé";
    public static final String FORMATION_LIBELLE_PYTHON = "Formation Python Expert";
    public static final String FORMATION_LIBELLE_REACT = "Formation React";

    // Formateurs constants
    public static final String FORMATEUR_JEAN_DUPONT = "Jean Dupont";
    public static final String FORMATEUR_MARIE_MARTIN = "Marie Martin";
    public static final String FORMATEUR_EXPERT_FRONTEND = "Expert Frontend";

    /**
     * Formation présentielle standard pour les tests.
     */
    public static Formation defaultFormationPresentiel() {
        return Formation.builder()
                .id(FORMATION_ID_1)
                .libelle(FORMATION_LIBELLE_JAVA)
                .formateurs(FORMATEUR_JEAN_DUPONT)
                .description("Formation approfondie sur Java Enterprise")
                .dateFormation(LocalDate.now().plusDays(15))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("Informatique")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Salle A")
                .ville("Paris")
                .build();
    }

    /**
     * Formation en ligne standard pour les tests.
     */
    public static Formation defaultFormationEnLigne() {
        return Formation.builder()
                .id(FORMATION_ID_2)
                .libelle(FORMATION_LIBELLE_PYTHON)
                .formateurs(FORMATEUR_MARIE_MARTIN)
                .description("Formation expert Python et frameworks modernes")
                .dateFormation(LocalDate.now().plusDays(20))
                .heureDebut(LocalTime.of(10, 0))
                .heureFin(LocalTime.of(16, 0))
                .secteur("Informatique")
                .region("Auvergne-Rhône-Alpes")
                .modalite(ModaliteFormation.EN_LIGNE)
                .nbParticipants(30)
                .lienParticipation("https://python.example.com")
                .build();
    }

    /**
     * Formation hybride standard pour les tests.
     */
    public static Formation defaultFormationHybride() {
        return Formation.builder()
                .id(FORMATION_ID_3)
                .libelle(FORMATION_LIBELLE_REACT)
                .formateurs(FORMATEUR_EXPERT_FRONTEND)
                .description("Formation React et Redux pour développeurs expérimentés")
                .dateFormation(LocalDate.now().plusDays(25))
                .heureDebut(LocalTime.of(9, 30))
                .heureFin(LocalTime.of(17, 30))
                .secteur("Informatique")
                .region("Nouvelle-Aquitaine")
                .modalite(ModaliteFormation.HYBRIDE)
                .nbParticipants(15)
                .lieu("Campus Innovation")
                .ville("Bordeaux")
                .lienParticipation("https://react.example.com")
                .build();
    }

    /**
     * Formation terminée (dans le passé).
     */
    public static Formation formationTerminee() {
        return defaultFormationPresentiel().toBuilder()
                .dateFormation(LocalDate.now().minusDays(5))
                .build();
    }

    /**
     * Formation en cours (aujourd'hui).
     */
    public static Formation formationEnCours() {
        return defaultFormationPresentiel().toBuilder()
                .dateFormation(LocalDate.now())
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(23, 59))
                .build();
    }

    /**
     * Formation à venir (dans le futur).
     */
    public static Formation formationAVenir() {
        return defaultFormationPresentiel().toBuilder()
                .dateFormation(LocalDate.now().plusDays(30))
                .build();
    }

    /**
     * Formation avec données minimales.
     */
    public static Formation formationMinimale() {
        return Formation.builder()
                .libelle("Formation Minimale")
                .formateurs("Formateur Test")
                .description("Description minimale")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("Test")
                .region("Test")
                .modalite(ModaliteFormation.EN_LIGNE)
                .nbParticipants(10)
                .lienParticipation("https://test.example.com")
                .build();
    }

    /**
     * Formation avec validation d'horaires invalides.
     */
    public static Formation formationHorairesInvalides() {
        return defaultFormationPresentiel().toBuilder()
                .heureDebut(LocalTime.of(17, 0))
                .heureFin(LocalTime.of(9, 0))
                .build();
    }

    /**
     * Formation présentielle sans lieu (pour tests de validation).
     */
    public static Formation formationPresentielSansLieu() {
        return defaultFormationPresentiel().toBuilder()
                .lieu(null)
                .ville(null)
                .build();
    }

    /**
     * Formation en ligne sans lien (pour tests de validation).
     */
    public static Formation formationEnLigneSansLien() {
        return defaultFormationEnLigne().toBuilder()
                .lienParticipation(null)
                .build();
    }
}