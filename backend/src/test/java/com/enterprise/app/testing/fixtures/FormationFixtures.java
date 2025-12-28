package com.enterprise.app.testing.fixtures;

import com.enterprise.app.application.dto.*;
import com.enterprise.app.domain.model.*;
import org.instancio.Instancio;
import static org.instancio.Select.field;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Fixtures centralisées pour les données de test Formation.
 * Fournit des instances prédéfinies et réutilisables pour les tests.
 */
public final class FormationFixtures {

    public static final String LIBELLE_FORMATION = "Tirage au sort - Sécrétaire";
    public static final String LIBELLE_FORMATION_2 = "Tirage au sort";

    public static final String FORMATEURS = "LAGRACE Elodie - DUPONT Frédérique";
    public static final String DESCRIPTION_FORMATION = "Tirage au sort - Régime général";

    public static final String LIEU_FORMATION_1 = "1 rue serpentine Courbevoie";
    public static final String LIEU_FORMATION_2 = "1 place du Fort";

    public static final String VILLE_FORMATION_1 = "Courbevoie";
    public static final String VILLE_FORMATION_2 = "Fort de France";

    public static final LocalTime HEURE_DEBUT = LocalTime.of(9, 0);
    public static final LocalTime HEURE_FIN = LocalTime.of(15, 0);
    public static final LocalTime HEURE_FIN_TARDIF = LocalTime.of(23, 50);
    public static final int NB_PARTICIPANTS = 30;
    public static final String LIBELLE_FORMATION_3 = "Tirage au sort - RU";

    private FormationFixtures() {}

    // IDs constants pour les tests
    public static final Long FORMATION_ID_1 = 1L;
    public static final Long FORMATION_ID_2 = 2L;
    public static final Long FORMATION_ID_3 = 3L;
    public static final Long FORMATION_ID_4 = 4L;

    // Secteurs et régions constants
    public static final Secteur SECTEUR_RG = Secteur.builder()
            .id(1L)
            .code("RG")
            .nom("Regime général")
            .description("Régime général de sécurité sociale")
            .actif(true)
            .build();

    public static final Secteur SECTEUR_MSA = Secteur.builder()
            .id(2L)
            .code("MSA")
            .nom("MSA")
            .description("Mutualité Sociale Agricole")
            .actif(true)
            .build();

    public static final Region REGION_IDF = Region.builder()
            .id(1L)
            .code("IDF")
            .nom("Île-de-France")
            .description("Région parisienne et départements limitrophes")
            .actif(true)
            .build();

    public static final Region REGION_AURA = Region.builder()
            .id(2L)
            .code("AURA")
            .nom("Auvergne-Rhône-Alpes")
            .description("Région du sud-est de la France")
            .actif(true)
            .build();

    public static final Region REGION_MARTINIQUE = Region.builder()
            .id(3L)
            .code("MTQ")
            .nom("Martinique")
            .description("Région d'outre-mer des Antilles")
            .actif(true)
            .build();

    /**
     * Formation "Tirage au sort" en présentiel - Régime général.
     */
    public static Formation defaultFormationPresentiel() {
        return Instancio.of(Formation.class)
                .set(field(Formation::getId), FORMATION_ID_1)
                .set(field(Formation::getLibelle), LIBELLE_FORMATION_2)
                .set(field(Formation::getFormateurs), FORMATEURS)
                .set(field(Formation::getDescription), DESCRIPTION_FORMATION)
                .set(field(Formation::getDateFormation), LocalDate.of(2026, 1, 15))
                .set(field(Formation::getHeureDebut), HEURE_DEBUT)
                .set(field(Formation::getHeureFin), HEURE_FIN)
                .set(field(Formation::getSecteur), SECTEUR_RG)
                .set(field(Formation::getRegion), REGION_IDF)
                .set(field(Formation::getModalite), ModaliteFormation.PRESENTIEL)
                .set(field(Formation::getNbParticipants), NB_PARTICIPANTS)
                .set(field(Formation::getLieu), LIEU_FORMATION_1)
                .set(field(Formation::getVille), VILLE_FORMATION_1)
                .set(field(Formation::getLienParticipation), null)
                .create();
    }

    /**
     * Formation "Tirage au sort" en ligne - Régime général.
     */
    public static Formation tirageAuSortEnLigneRG() {
        return Instancio.of(Formation.class)
            .set(field(Formation::getId), FORMATION_ID_2)
            .set(field(Formation::getLibelle), LIBELLE_FORMATION_2)
            .set(field(Formation::getFormateurs), FORMATEURS)
            .set(field(Formation::getDescription), DESCRIPTION_FORMATION)
            .set(field(Formation::getDateFormation), LocalDate.of(2026, 1, 15))
            .set(field(Formation::getHeureDebut), HEURE_DEBUT)
            .set(field(Formation::getHeureFin), HEURE_FIN)
            .set(field(Formation::getSecteur), SECTEUR_RG)
            .set(field(Formation::getRegion), REGION_IDF)
            .set(field(Formation::getModalite), ModaliteFormation.EN_LIGNE)
            .set(field(Formation::getNbParticipants), NB_PARTICIPANTS)
            .set(field(Formation::getLieu), null)
            .set(field(Formation::getVille), null)
            .set(field(Formation::getLienParticipation), "https://formation.link/12345")
            .create();
    }

    /**
     * Formation "Tirage au sort - RU" en présentiel - MSA.
     */
    public static Formation tirageAuSortRU() {
        return Instancio.of(Formation.class)
                .set(field(Formation::getId), FORMATION_ID_3)
                .set(field(Formation::getLibelle), LIBELLE_FORMATION_3)
                .set(field(Formation::getFormateurs), FORMATEURS)
                .set(field(Formation::getDescription), DESCRIPTION_FORMATION)
                .set(field(Formation::getDateFormation), LocalDate.of(2026, 1, 15))
                .set(field(Formation::getHeureDebut), HEURE_DEBUT)
                .set(field(Formation::getHeureFin), HEURE_FIN)
                .set(field(Formation::getSecteur), SECTEUR_MSA)
                .set(field(Formation::getRegion), REGION_IDF)
                .set(field(Formation::getModalite), ModaliteFormation.PRESENTIEL)
                .set(field(Formation::getNbParticipants), NB_PARTICIPANTS)
                .set(field(Formation::getLieu), LIEU_FORMATION_1)
                .set(field(Formation::getVille), VILLE_FORMATION_1)
                .set(field(Formation::getLienParticipation), null)
                .create();
    }

    /**
     * Formation "Tirage au sort - Sécrétaire" en présentiel - Martinique.
     */
    public static Formation tirageAuSortSecretaireEnCours() {
        return Instancio.of(Formation.class)
                .set(field(Formation::getId), FORMATION_ID_4)
                .set(field(Formation::getLibelle), LIBELLE_FORMATION)
                .set(field(Formation::getFormateurs), FORMATEURS)
                .set(field(Formation::getDescription), DESCRIPTION_FORMATION)
                .set(field(Formation::getDateFormation), LocalDate.now())
                .set(field(Formation::getHeureDebut), HEURE_DEBUT)
                .set(field(Formation::getHeureFin), HEURE_FIN_TARDIF)
                .set(field(Formation::getSecteur), SECTEUR_RG)
                .set(field(Formation::getRegion), REGION_MARTINIQUE)
                .set(field(Formation::getModalite), ModaliteFormation.PRESENTIEL)
                .set(field(Formation::getNbParticipants), NB_PARTICIPANTS)
                .set(field(Formation::getLieu), LIEU_FORMATION_2)
                .set(field(Formation::getVille), VILLE_FORMATION_2)
                .set(field(Formation::getLienParticipation), null)
                .create();
    }

    /**
     * Crée un FormationDTO par défaut.
     */
    public static FormationDTO createDefaultFormationDTO() {
        Formation formation = FormationFixtures.defaultFormationPresentiel();
        return FormationDTO.builder()
                .id(formation.getId())
                .libelle(formation.getLibelle())
                .formateurs(formation.getFormateurs())
                .description(formation.getDescription())
                .dateFormation(formation.getDateFormation())
                .heureDebut(formation.getHeureDebut())
                .heureFin(formation.getHeureFin())
                .secteur(convertSecteurToDTO(formation.getSecteur()))
                .region(convertRegionToDTO(formation.getRegion()))
                .modalite(formation.getModalite())
                .nbParticipants(formation.getNbParticipants())
                .lieu(formation.getLieu())
                .ville(formation.getVille())
                .lienParticipation(formation.getLienParticipation())
                .statut(FormationStatut.A_VENIR)
                .nbParticipantsInscrits(5)
                .complet(false)
                .build();
    }

    /**
     * Crée un CreateFormationRequest par défaut.
     */
    public static CreateFormationRequest createDefaultFormationRequest() {
        Formation formation = FormationFixtures.defaultFormationPresentiel();
        return CreateFormationRequest.builder()
            .libelle(formation.getLibelle())
            .formateurs(formation.getFormateurs())
            .description(formation.getDescription())
            .dateFormation(formation.getDateFormation())
            .heureDebut(formation.getHeureDebut())
            .heureFin(formation.getHeureFin())
            .secteurId(formation.getSecteur().getId())
            .regionId(formation.getRegion().getId())
            .modalite(formation.getModalite())
            .nbParticipants(formation.getNbParticipants())
            .lieu(formation.getLieu())
            .ville(formation.getVille())
            .lienParticipation(formation.getLienParticipation())
            .build();
    }

    public static CreateFormationRequest createDefaultFormationEnLigneRequest() {
        Formation formation = FormationFixtures.tirageAuSortEnLigneRG();
        return CreateFormationRequest.builder()
                .libelle(formation.getLibelle())
                .formateurs(formation.getFormateurs())
                .description(formation.getDescription())
                .dateFormation(formation.getDateFormation())
                .heureDebut(formation.getHeureDebut())
                .heureFin(formation.getHeureFin())
                .secteurId(formation.getSecteur().getId())
                .regionId(formation.getRegion().getId())
                .modalite(formation.getModalite())
                .nbParticipants(formation.getNbParticipants())
                .lieu(formation.getLieu())
                .ville(formation.getVille())
                .lienParticipation(formation.getLienParticipation())
                .build();
    }

    /**
     * Crée un FormationDTO par défaut.
     */
    public static UpdateFormationRequest createDefaultFormationUpdateRequest() {
        Formation formation = FormationFixtures.tirageAuSortEnLigneRG();
        return UpdateFormationRequest.builder()
                .libelle(formation.getLibelle())
                .formateurs(formation.getFormateurs())
                .description(formation.getDescription())
                .dateFormation(formation.getDateFormation())
                .heureDebut(formation.getHeureDebut())
                .heureFin(formation.getHeureFin())
                .secteurId(formation.getSecteur().getId())
                .regionId(formation.getRegion().getId())
                .modalite(formation.getModalite())
                .nbParticipants(formation.getNbParticipants())
                .lieu(formation.getLieu())
                .ville(formation.getVille())
                .lienParticipation(formation.getLienParticipation())
                .build();
    }

    /**
     * Convertit un Secteur en SecteurDTO.
     */
    private static SecteurDTO convertSecteurToDTO(Secteur secteur) {
        if (secteur == null) return null;
        return SecteurDTO.builder()
                .id(secteur.getId())
                .code(secteur.getCode())
                .nom(secteur.getNom())
                .description(secteur.getDescription())
                .actif(secteur.getActif())
                .build();
    }

    /**
     * Convertit une Region en RegionDTO.
     */
    private static RegionDTO convertRegionToDTO(Region region) {
        if (region == null) return null;
        return RegionDTO.builder()
                .id(region.getId())
                .code(region.getCode())
                .nom(region.getNom())
                .description(region.getDescription())
                .actif(region.getActif())
                .build();
    }
}