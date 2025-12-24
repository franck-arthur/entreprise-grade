package com.enterprise.app.testing.helpers;

import com.enterprise.app.application.dto.FormationDTO;
import com.enterprise.app.application.service.FormationService;
import com.enterprise.app.domain.model.Formation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Utilitaires de test centralisés pour les formations.
 * Fournit des méthodes réutilisables pour les assertions et vérifications communes.
 */
public class FormationTestHelper {

    /**
     * Vérifie l'égalité entre deux formations (champs principaux).
     */
    public static void assertFormationEquals(Formation expected, Formation actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getLibelle()).isEqualTo(expected.getLibelle());
        assertThat(actual.getFormateurs()).isEqualTo(expected.getFormateurs());
        assertThat(actual.getDateFormation()).isEqualTo(expected.getDateFormation());
        assertThat(actual.getModalite()).isEqualTo(expected.getModalite());
        assertThat(actual.getSecteur()).isEqualTo(expected.getSecteur());
        assertThat(actual.getRegion()).isEqualTo(expected.getRegion());
    }

    /**
     * Vérifie l'égalité entre deux DTOs de formation.
     */
    public static void assertFormationDTOEquals(FormationDTO expected, FormationDTO actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getLibelle()).isEqualTo(expected.getLibelle());
        assertThat(actual.getFormateurs()).isEqualTo(expected.getFormateurs());
        assertThat(actual.getDateFormation()).isEqualTo(expected.getDateFormation());
        assertThat(actual.getModalite()).isEqualTo(expected.getModalite());
        assertThat(actual.getStatut()).isEqualTo(expected.getStatut());
    }

    /**
     * Vérifie qu'une formation contient les données essentielles.
     */
    public static void assertFormationIsValid(Formation formation) {
        assertThat(formation).isNotNull();
        assertThat(formation.getLibelle()).isNotBlank();
        assertThat(formation.getFormateurs()).isNotBlank();
        assertThat(formation.getDateFormation()).isNotNull();
        assertThat(formation.getHeureDebut()).isNotNull();
        assertThat(formation.getHeureFin()).isNotNull();
        assertThat(formation.getModalite()).isNotNull();
        assertThat(formation.getNbParticipants()).isPositive();
    }

    /**
     * Retourne un tableau de ResultMatcher pour vérifier une formation dans une réponse JSON.
     */
    public static ResultMatcher[] standardFormationMatchers(Formation formation) {
        return new ResultMatcher[] {
            jsonPath("$.id").value(formation.getId().toString()),
            jsonPath("$.libelle").value(formation.getLibelle()),
            jsonPath("$.formateurs").value(formation.getFormateurs()),
            jsonPath("$.modalite").value(formation.getModalite().toString()),
            jsonPath("$.secteur").value(formation.getSecteur()),
            jsonPath("$.region").value(formation.getRegion())
        };
    }

    /**
     * Retourne un tableau de ResultMatcher pour vérifier un DTO de formation dans une réponse JSON.
     */
    public static ResultMatcher[] standardFormationDTOMatchers(FormationDTO formationDTO) {
        return new ResultMatcher[] {
            jsonPath("$.id").value(formationDTO.getId().toString()),
            jsonPath("$.libelle").value(formationDTO.getLibelle()),
            jsonPath("$.formateurs").value(formationDTO.getFormateurs()),
            jsonPath("$.modalite").value(formationDTO.getModalite().toString()),
            jsonPath("$.statut").value(formationDTO.getStatut().toString()),
            jsonPath("$.nbParticipantsInscrits").value(formationDTO.getNbParticipantsInscrits()),
            jsonPath("$.complet").value(formationDTO.isComplet())
        };
    }

    /**
     * Vérifie les interactions standard avec le service Formation (lecture seule).
     */
    public static void verifyStandardFormationServiceCall(FormationService service, UUID formationId) {
        verify(service).getFormationById(formationId);
        verifyNoMoreInteractions(service);
    }

    /**
     * Vérifie les interactions standard de création avec le service Formation.
     */
    public static void verifyStandardCreateServiceCall(FormationService service) {
        verify(service).createFormation(org.mockito.ArgumentMatchers.any(Formation.class));
        verifyNoMoreInteractions(service);
    }

    /**
     * Vérifie les interactions standard de mise à jour avec le service Formation.
     */
    public static void verifyStandardUpdateServiceCall(FormationService service, UUID formationId) {
        verify(service).getFormationById(formationId);
        verify(service).updateFormation(org.mockito.ArgumentMatchers.eq(formationId),
                                       org.mockito.ArgumentMatchers.any(Formation.class));
        verifyNoMoreInteractions(service);
    }

    /**
     * Crée une page de formations pour les tests de pagination.
     */
    public static Page<Formation> createFormationPage(List<Formation> formations, Pageable pageable) {
        return new PageImpl<>(formations, pageable, formations.size());
    }

    /**
     * Crée une page de DTOs pour les tests de pagination.
     */
    public static Page<FormationDTO> createFormationDTOPage(List<FormationDTO> formations, Pageable pageable) {
        return new PageImpl<>(formations, pageable, formations.size());
    }

    /**
     * Vérifie qu'une page contient le nombre attendu d'éléments.
     */
    public static void assertPageContent(Page<?> page, int expectedSize) {
        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(expectedSize);
        assertThat(page.getTotalElements()).isEqualTo(expectedSize);
    }

    /**
     * Vérifie qu'une page est vide.
     */
    public static void assertPageIsEmpty(Page<?> page) {
        assertPageContent(page, 0);
    }

    /**
     * Vérifie les propriétés de pagination d'une page.
     */
    public static void assertPageProperties(Page<?> page, int expectedSize, int expectedTotalPages, boolean isFirst, boolean isLast) {
        assertThat(page.getSize()).isEqualTo(expectedSize);
        assertThat(page.getTotalPages()).isEqualTo(expectedTotalPages);
        assertThat(page.isFirst()).isEqualTo(isFirst);
        assertThat(page.isLast()).isEqualTo(isLast);
    }

    /**
     * Vérifie qu'une formation respecte les règles de modalité présentielle.
     */
    public static void assertFormationPresentielleIsValid(Formation formation) {
        assertFormationIsValid(formation);
        assertThat(formation.getModalite()).isEqualTo(com.enterprise.app.domain.model.ModaliteFormation.PRESENTIEL);
        assertThat(formation.getLieu()).isNotBlank();
        assertThat(formation.getVille()).isNotBlank();
    }

    /**
     * Vérifie qu'une formation respecte les règles de modalité en ligne.
     */
    public static void assertFormationEnLigneIsValid(Formation formation) {
        assertFormationIsValid(formation);
        assertThat(formation.getModalite()).isEqualTo(com.enterprise.app.domain.model.ModaliteFormation.EN_LIGNE);
        assertThat(formation.getLienParticipation()).isNotBlank();
    }

    /**
     * Vérifie qu'une formation respecte les règles de modalité hybride.
     */
    public static void assertFormationHybrideIsValid(Formation formation) {
        assertFormationIsValid(formation);
        assertThat(formation.getModalite()).isEqualTo(com.enterprise.app.domain.model.ModaliteFormation.HYBRIDE);
        assertThat(formation.getLieu()).isNotBlank();
        assertThat(formation.getVille()).isNotBlank();
        assertThat(formation.getLienParticipation()).isNotBlank();
    }

    /**
     * Vérifie que les heures d'une formation sont cohérentes.
     */
    public static void assertFormationHorairesValides(Formation formation) {
        assertThat(formation.getHeureDebut()).isNotNull();
        assertThat(formation.getHeureFin()).isNotNull();
        assertThat(formation.getHeureDebut()).isBefore(formation.getHeureFin());
    }

    /**
     * Crée un message d'erreur formaté pour les assertions de formation.
     */
    public static String formatFormationErrorMessage(String operation, Formation formation) {
        return String.format("Erreur lors de %s pour la formation: %s (ID: %s)",
                            operation, formation.getLibelle(), formation.getId());
    }
}