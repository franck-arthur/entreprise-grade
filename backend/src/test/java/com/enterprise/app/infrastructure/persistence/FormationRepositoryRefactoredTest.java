package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.testing.config.BaseIntegrationTest;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import com.enterprise.app.testing.fixtures.SecteurFixtures;
import com.enterprise.app.testing.builders.FormationTestDataBuilder;
import com.enterprise.app.testing.helpers.FormationTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test d'intégration refactorisé pour FormationRepository utilisant l'architecture centralisée.
 */
@DisplayName("Formation Repository Integration - Tests Refactorisés")
class FormationRepositoryRefactoredTest extends BaseIntegrationTest {

    @Autowired
    private FormationRepositoryImpl formationRepository;

    @Autowired
    private JpaFormationRepository jpaFormationRepository;

    @Override
    protected void cleanupDatabase() {
        jpaFormationRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save and retrieve formation successfully")
    void save_ShouldPersistFormation() {
        // Given
        Formation formation = FormationFixtures.defaultFormationPresentiel();

        // When
        Formation savedFormation = formationRepository.save(formation);

        // Then
        assertThat(savedFormation.getId()).isNotNull();
        FormationTestHelper.assertFormationPresentielleIsValid(savedFormation);

        Optional<Formation> retrieved = formationRepository.findById(savedFormation.getId());
        assertThat(retrieved).isPresent();
        FormationTestHelper.assertFormationEquals(formation, retrieved.get());
    }

    @Test
    @DisplayName("Should apply complex filters correctly")
    void findByFilters_ShouldApplyMultipleFilters() {
        // Given
        Formation targetFormation = FormationTestDataBuilder.aFormation()
                .withSecteur(FormationFixtures.SECTEUR_MSA)
                .withRegion(FormationFixtures.REGION_IDF)
                .presentiel()
                .aVenir()
                .build();

        Formation differentSecteur = FormationTestDataBuilder.aFormation()
                .withSecteur(SecteurFixtures.secteurWithNom("Marketing"))
                .withRegion(FormationFixtures.REGION_IDF)
                .presentiel()
                .aVenir()
                .build();

        Formation differentModalite = FormationTestDataBuilder.aFormation()
                .withSecteur(FormationFixtures.SECTEUR_MSA)
                .withRegion(FormationFixtures.REGION_IDF)
                .enLigne()
                .aVenir()
                .build();

        formationRepository.save(targetFormation);
        formationRepository.save(differentSecteur);
        formationRepository.save(differentModalite);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> result = formationRepository.findByFilters(
                FormationFixtures.SECTEUR_MSA.getId(), FormationFixtures.REGION_IDF.getId(), ModaliteFormation.PRESENTIEL, FormationStatut.A_VENIR, pageable);

        // Then
        FormationTestHelper.assertPageContent(result, 1);
        Formation found = result.getContent().get(0);
        assertThat(found.getSecteur()).isEqualTo(FormationFixtures.SECTEUR_MSA);
        assertThat(found.getRegion()).isEqualTo(FormationFixtures.REGION_IDF);
        assertThat(found.getModalite()).isEqualTo(ModaliteFormation.PRESENTIEL);
        assertThat(found.getStatut()).isEqualTo(FormationStatut.A_VENIR);
    }

    @Test
    @DisplayName("Should filter by statut using business logic")
    void findByFilters_ShouldFilterByStatut() {
        // Given
        Formation formationPast = FormationTestDataBuilder.aFormation()
                .terminee()
                .build();

        Formation formationFuture = FormationTestDataBuilder.aFormation()
                .aVenir()
                .build();

        Formation formationCurrent = FormationTestDataBuilder.aFormation()
                .enCours()
                .build();

        formationRepository.save(formationPast);
        formationRepository.save(formationFuture);
        formationRepository.save(formationCurrent);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> futureFormations = formationRepository.findByFilters(
                null, null, null, FormationStatut.A_VENIR, pageable);

        Page<Formation> pastFormations = formationRepository.findByFilters(
                null, null, null, FormationStatut.TERMINEE, pageable);

        Page<Formation> currentFormations = formationRepository.findByFilters(
                null, null, null, FormationStatut.EN_COURS, pageable);

        // Then
        FormationTestHelper.assertPageContent(futureFormations, 1);
        assertThat(futureFormations.getContent().get(0).getStatut()).isEqualTo(FormationStatut.A_VENIR);

        FormationTestHelper.assertPageContent(pastFormations, 1);
        assertThat(pastFormations.getContent().get(0).getStatut()).isEqualTo(FormationStatut.TERMINEE);

        FormationTestHelper.assertPageContent(currentFormations, 1);
        assertThat(currentFormations.getContent().get(0).getStatut()).isEqualTo(FormationStatut.EN_COURS);
    }

    @Test
    @DisplayName("Should validate formation data before saving")
    void save_ShouldValidateFormationData() {
        // Given
        Formation invalidFormation = FormationTestDataBuilder.aFormation()
                .avecHorairesInvalides()
                .buildWithoutValidation();

        // When & Then
        assertThatThrownBy(() -> formationRepository.save(invalidFormation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heure de fin doit être postérieure");
    }

    @Test
    @DisplayName("Should delete formation correctly")
    void deleteById_ShouldRemoveFormation() {
        // Given
        Formation formation = FormationFixtures.defaultFormationPresentiel();
        Formation savedFormation = formationRepository.save(formation);

        // When
        formationRepository.deleteById(savedFormation.getId());

        // Then
        Optional<Formation> deleted = formationRepository.findById(savedFormation.getId());
        assertThat(deleted).isEmpty();
        assertThat(formationRepository.existsById(savedFormation.getId())).isFalse();
    }

    @Test
    @DisplayName("Should save and retrieve 'Tirage au sort' formation")
    void save_ShouldPersistTirageAuSortFormation() {
        // Given
        Formation tirageAuSort = FormationFixtures.defaultFormationPresentiel();

        // When
        Formation savedFormation = formationRepository.save(tirageAuSort);

        // Then
        assertThat(savedFormation.getId()).isNotNull();
        assertThat(savedFormation.getLibelle()).isEqualTo("Tirage au sort");
        assertThat(savedFormation.getFormateurs()).isEqualTo("LAGRACE Elodie - DUPONT Frédérique");
        assertThat(savedFormation.getSecteur()).isEqualTo(FormationFixtures.SECTEUR_RG);
        assertThat(savedFormation.getRegion()).isEqualTo(FormationFixtures.REGION_IDF);
        assertThat(savedFormation.getNbParticipants()).isEqualTo(30);

        Optional<Formation> retrieved = formationRepository.findById(savedFormation.getId());
        assertThat(retrieved).isPresent();
        FormationTestHelper.assertFormationEquals(tirageAuSort, retrieved.get());
    }

    @Test
    @DisplayName("Should apply complex filters for new formations data")
    void findByFilters_ShouldWorkWithTirageAuSortData() {
        // Given
        Formation targetFormation = FormationFixtures.defaultFormationPresentiel();
        Formation msaFormation = FormationFixtures.tirageAuSortRU();
        Formation onlineFormation = FormationFixtures.tirageAuSortEnLigneRG();

        formationRepository.save(targetFormation);
        formationRepository.save(msaFormation);
        formationRepository.save(onlineFormation);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> regimeGeneralPresentielIDF = formationRepository.findByFilters(
                FormationFixtures.SECTEUR_RG.getId(), FormationFixtures.REGION_IDF.getId(), ModaliteFormation.PRESENTIEL, null, pageable);

        Page<Formation> msaFormations = formationRepository.findByFilters(
                FormationFixtures.SECTEUR_MSA.getId(), FormationFixtures.REGION_IDF.getId(), ModaliteFormation.PRESENTIEL, null, pageable);

        // Then
        FormationTestHelper.assertPageContent(regimeGeneralPresentielIDF, 1);
        Formation foundRG = regimeGeneralPresentielIDF.getContent().get(0);
        assertThat(foundRG.getSecteur()).isEqualTo(FormationFixtures.SECTEUR_RG);
        assertThat(foundRG.getRegion()).isEqualTo(FormationFixtures.REGION_IDF);
        assertThat(foundRG.getModalite()).isEqualTo(ModaliteFormation.PRESENTIEL);
        assertThat(foundRG.getLibelle()).isEqualTo("Tirage au sort");

        FormationTestHelper.assertPageContent(msaFormations, 1);
        Formation foundMSA = msaFormations.getContent().get(0);
        assertThat(foundMSA.getSecteur()).isEqualTo(FormationFixtures.SECTEUR_MSA);
        assertThat(foundMSA.getLibelle()).isEqualTo("Tirage au sort - RU");
    }
}