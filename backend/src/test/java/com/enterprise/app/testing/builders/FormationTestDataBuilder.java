package com.enterprise.app.testing.builders;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.domain.model.Secteur;
import com.enterprise.app.domain.model.Region;
import com.enterprise.app.testing.fixtures.FormationFixtures;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Builder pour créer des instances de Formation personnalisées pour les tests.
 * Utilise le pattern Builder pour une construction flexible et lisible.
 */
public class FormationTestDataBuilder {

    private Long id = 1L;
    private String libelle = "Formation Test";
    private String formateurs = "Formateur Test";
    private String description = "Description test pour formation";
    private LocalDate dateFormation = LocalDate.now().plusDays(7);
    private LocalTime heureDebut = LocalTime.of(9, 0);
    private LocalTime heureFin = LocalTime.of(17, 0);
    private Secteur secteur = FormationFixtures.SECTEUR_MSA;
    private Region region = FormationFixtures.REGION_IDF;
    private ModaliteFormation modalite = ModaliteFormation.PRESENTIEL;
    private Integer nbParticipants = 20;
    private String lieu = "Salle Test";
    private String ville = "Paris";
    private String lienParticipation;

    /**
     * Point d'entrée pour créer un builder.
     */
    public static FormationTestDataBuilder aFormation() {
        return new FormationTestDataBuilder();
    }

    /**
     * Définit l'ID de la formation.
     */
    public FormationTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Définit le libellé de la formation.
     */
    public FormationTestDataBuilder withLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    /**
     * Définit les formateurs.
     */
    public FormationTestDataBuilder withFormateurs(String formateurs) {
        this.formateurs = formateurs;
        return this;
    }

    /**
     * Définit la description.
     */
    public FormationTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Définit la date de formation.
     */
    public FormationTestDataBuilder withDateFormation(LocalDate dateFormation) {
        this.dateFormation = dateFormation;
        return this;
    }

    /**
     * Définit l'heure de début.
     */
    public FormationTestDataBuilder withHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
        return this;
    }

    /**
     * Définit l'heure de fin.
     */
    public FormationTestDataBuilder withHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
        return this;
    }

    /**
     * Définit le secteur.
     */
    public FormationTestDataBuilder withSecteur(Secteur secteur) {
        this.secteur = secteur;
        return this;
    }

    /**
     * Définit la région.
     */
    public FormationTestDataBuilder withRegion(Region region) {
        this.region = region;
        return this;
    }

    /**
     * Définit le secteur par String (pour compatibilité).
     * @deprecated Utiliser withSecteur(Secteur) à la place
     */
    @Deprecated
    public FormationTestDataBuilder withSecteur(String secteurNom) {
        switch (secteurNom) {
            case "Regime général", "RG" ->
                this.secteur = FormationFixtures.SECTEUR_RG;
            case "MSA" ->
                this.secteur = FormationFixtures.SECTEUR_MSA;
            default ->
                // Pour les cas non mappés, créer un secteur dynamique
                this.secteur = Secteur.builder()
                    .nom(secteurNom)
                    .code(secteurNom.toUpperCase().substring(0, Math.min(3, secteurNom.length())))
                    .actif(true)
                    .build();
        }
        return this;
    }

    /**
     * Définit la région par String (pour compatibilité).
     * @deprecated Utiliser withRegion(Region) à la place
     */
    @Deprecated
    public FormationTestDataBuilder withRegion(String regionNom) {
        switch (regionNom) {
            case "Île-de-France", "IDF" ->
                this.region = FormationFixtures.REGION_IDF;
            case "Auvergne-Rhône-Alpes", "AURA" ->
                this.region = FormationFixtures.REGION_AURA;
            case "Martinique", "MTQ" ->
                this.region = FormationFixtures.REGION_MARTINIQUE;
            default ->
                // Pour les cas non mappés, créer une région dynamique
                this.region = Region.builder()
                    .nom(regionNom)
                    .code(regionNom.toUpperCase().substring(0, Math.min(3, regionNom.length())))
                    .actif(true)
                    .build();
        }
        return this;
    }

    /**
     * Définit la modalité.
     */
    public FormationTestDataBuilder withModalite(ModaliteFormation modalite) {
        this.modalite = modalite;
        if (modalite == ModaliteFormation.EN_LIGNE) {
            this.lienParticipation = "https://example.com";
            this.lieu = null;
            this.ville = null;
        }
        return this;
    }

    /**
     * Définit le nombre de participants.
     */
    public FormationTestDataBuilder withNbParticipants(Integer nbParticipants) {
        this.nbParticipants = nbParticipants;
        return this;
    }

    /**
     * Définit le lieu.
     */
    public FormationTestDataBuilder withLieu(String lieu) {
        this.lieu = lieu;
        return this;
    }

    /**
     * Définit la ville.
     */
    public FormationTestDataBuilder withVille(String ville) {
        this.ville = ville;
        return this;
    }

    /**
     * Définit le lien de participation.
     */
    public FormationTestDataBuilder withLienParticipation(String lienParticipation) {
        this.lienParticipation = lienParticipation;
        return this;
    }

    // Méthodes de convenance pour les configurations courantes

    /**
     * Configure une formation présentielle avec lieu et ville.
     */
    public FormationTestDataBuilder presentiel() {
        return withModalite(ModaliteFormation.PRESENTIEL)
                .withLieu("Salle de formation")
                .withVille("Paris");
    }

    /**
     * Configure une formation en ligne avec lien.
     */
    public FormationTestDataBuilder enLigne() {
        return withModalite(ModaliteFormation.EN_LIGNE)
                .withLienParticipation("https://formation.example.com");
    }

    /**
     * Configure une formation terminée (dans le passé).
     */
    public FormationTestDataBuilder terminee() {
        return withDateFormation(LocalDate.now().minusDays(5));
    }

    /**
     * Configure une formation à venir.
     */
    public FormationTestDataBuilder aVenir() {
        return withDateFormation(LocalDate.now().plusDays(15));
    }

    /**
     * Configure une formation en cours.
     */
    public FormationTestDataBuilder enCours() {
        return withDateFormation(LocalDate.now())
                .withHeureDebut(LocalTime.of(9, 0))
                .withHeureFin(LocalTime.of(23, 59));
    }

    /**
     * Configure une formation complète (nombre max de participants).
     */
    public FormationTestDataBuilder complete() {
        return withNbParticipants(1); // Will be full after one registration
    }

    /**
     * Configure une formation avec horaires invalides.
     */
    public FormationTestDataBuilder avecHorairesInvalides() {
        return withHeureDebut(LocalTime.of(17, 0))
                .withHeureFin(LocalTime.of(9, 0));
    }

    /**
     * Construit l'instance Formation avec validation.
     */
    public Formation build() {
        validateFormation();
        return Formation.builder()
                .id(id)
                .libelle(libelle)
                .formateurs(formateurs)
                .description(description)
                .dateFormation(dateFormation)
                .heureDebut(heureDebut)
                .heureFin(heureFin)
                .secteur(secteur)
                .region(region)
                .modalite(modalite)
                .nbParticipants(nbParticipants)
                .lieu(lieu)
                .ville(ville)
                .lienParticipation(lienParticipation)
                .build();
    }

    /**
     * Construit l'instance sans validation (pour tester les cas d'erreur).
     */
    public Formation buildWithoutValidation() {
        return Formation.builder()
                .id(id)
                .libelle(libelle)
                .formateurs(formateurs)
                .description(description)
                .dateFormation(dateFormation)
                .heureDebut(heureDebut)
                .heureFin(heureFin)
                .secteur(secteur)
                .region(region)
                .modalite(modalite)
                .nbParticipants(nbParticipants)
                .lieu(lieu)
                .ville(ville)
                .lienParticipation(lienParticipation)
                .build();
    }

    /**
     * Valide la cohérence des données de formation.
     */
    private void validateFormation() {
        if (modalite == ModaliteFormation.PRESENTIEL && (lieu == null || ville == null)) {
            throw new IllegalStateException("Formation présentielle doit avoir lieu et ville définis");
        }
        if (modalite == ModaliteFormation.EN_LIGNE && lienParticipation == null) {
            throw new IllegalStateException("Formation en ligne doit avoir un lien de participation");
        }
        if (heureDebut != null && heureFin != null && heureDebut.isAfter(heureFin)) {
            throw new IllegalStateException("L'heure de début doit être antérieure à l'heure de fin");
        }
        if (nbParticipants != null && nbParticipants <= 0) {
            throw new IllegalStateException("Le nombre de participants doit être positif");
        }
    }
}