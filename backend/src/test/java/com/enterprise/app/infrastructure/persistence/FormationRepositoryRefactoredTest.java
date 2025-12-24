package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.testing.config.BaseIntegrationTest;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import com.enterprise.app.testing.builders.FormationTestDataBuilder;
import com.enterprise.app.testing.helpers.FormationTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
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
    @DisplayName("Should find formations by secteur with pagination")
    void findBySecteur_ShouldReturnFilteredResults() {
        // Given
        Formation itFormation1 = FormationTestDataBuilder.aFormation()
                .withSecteur("Informatique")
                .withLibelle("Formation Java")
                .presentiel()
                .build();

        Formation itFormation2 = FormationTestDataBuilder.aFormation()
                .withSecteur("Informatique")
                .withLibelle("Formation Python")
                .enLigne()
                .build();

        Formation marketingFormation = FormationTestDataBuilder.aFormation()
                .withSecteur("Marketing")
                .withLibelle("Formation Marketing Digital")
                .presentiel()
                .build();

        formationRepository.save(itFormation1);
        formationRepository.save(itFormation2);
        formationRepository.save(marketingFormation);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> itFormations = formationRepository.findBySecteur("Informatique", pageable);
        Page<Formation> marketingFormations = formationRepository.findBySecteur("Marketing", pageable);

        // Then
        FormationTestHelper.assertPageContent(itFormations, 2);
        assertThat(itFormations.getContent())
                .extracting(Formation::getSecteur)
                .containsOnly("Informatique");

        FormationTestHelper.assertPageContent(marketingFormations, 1);
        assertThat(marketingFormations.getContent().get(0).getSecteur()).isEqualTo("Marketing");
    }

    @Test
    @DisplayName("Should find formations by modalite")
    void findByModalite_ShouldReturnFilteredResults() {
        // Given
        Formation presentielFormation = FormationTestDataBuilder.aFormation()
                .presentiel()
                .build();

        Formation enLigneFormation = FormationTestDataBuilder.aFormation()
                .enLigne()
                .build();

        Formation hybrideFormation = FormationTestDataBuilder.aFormation()
                .hybride()
                .build();

        formationRepository.save(presentielFormation);
        formationRepository.save(enLigneFormation);
        formationRepository.save(hybrideFormation);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> presentielResults = formationRepository.findByModalite(ModaliteFormation.PRESENTIEL, pageable);
        Page<Formation> enLigneResults = formationRepository.findByModalite(ModaliteFormation.EN_LIGNE, pageable);
        Page<Formation> hybrideResults = formationRepository.findByModalite(ModaliteFormation.HYBRIDE, pageable);

        // Then
        FormationTestHelper.assertPageContent(presentielResults, 1);
        assertThat(presentielResults.getContent().get(0).getModalite()).isEqualTo(ModaliteFormation.PRESENTIEL);

        FormationTestHelper.assertPageContent(enLigneResults, 1);
        assertThat(enLigneResults.getContent().get(0).getModalite()).isEqualTo(ModaliteFormation.EN_LIGNE);

        FormationTestHelper.assertPageContent(hybrideResults, 1);
        assertThat(hybrideResults.getContent().get(0).getModalite()).isEqualTo(ModaliteFormation.HYBRIDE);
    }

    @Test
    @DisplayName("Should find formations by date range")
    void findByDateFormationBetween_ShouldReturnFilteredResults() {
        // Given
        Formation formation1 = FormationTestDataBuilder.aFormation()
                .withDateFormation(LocalDate.now().plusDays(10))
                .withLibelle("Formation 1")
                .build();

        Formation formation2 = FormationTestDataBuilder.aFormation()
                .withDateFormation(LocalDate.now().plusDays(20))
                .withLibelle("Formation 2")
                .build();

        Formation formation3 = FormationTestDataBuilder.aFormation()
                .withDateFormation(LocalDate.now().plusDays(30))
                .withLibelle("Formation 3")
                .build();

        formationRepository.save(formation1);
        formationRepository.save(formation2);
        formationRepository.save(formation3);

        LocalDate startDate = LocalDate.now().plusDays(15);
        LocalDate endDate = LocalDate.now().plusDays(25);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> formationsInRange = formationRepository.findByDateFormationBetween(startDate, endDate, pageable);

        // Then
        FormationTestHelper.assertPageContent(formationsInRange, 1);
        assertThat(formationsInRange.getContent().get(0).getLibelle()).isEqualTo("Formation 2");
    }

    @Test
    @DisplayName("Should apply complex filters correctly")
    void findByFilters_ShouldApplyMultipleFilters() {
        // Given
        Formation targetFormation = FormationTestDataBuilder.aFormation()
                .withSecteur("Informatique")
                .withRegion("Île-de-France")
                .presentiel()
                .aVenir()
                .build();

        Formation differentSecteur = FormationTestDataBuilder.aFormation()
                .withSecteur("Marketing")
                .withRegion("Île-de-France")
                .presentiel()
                .aVenir()
                .build();

        Formation differentModalite = FormationTestDataBuilder.aFormation()
                .withSecteur("Informatique")
                .withRegion("Île-de-France")
                .enLigne()
                .aVenir()
                .build();

        formationRepository.save(targetFormation);
        formationRepository.save(differentSecteur);
        formationRepository.save(differentModalite);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> result = formationRepository.findByFilters(
                "Informatique", "Île-de-France", ModaliteFormation.PRESENTIEL, FormationStatut.A_VENIR, pageable);

        // Then
        FormationTestHelper.assertPageContent(result, 1);
        Formation found = result.getContent().get(0);
        assertThat(found.getSecteur()).isEqualTo("Informatique");
        assertThat(found.getRegion()).isEqualTo("Île-de-France");
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
    @DisplayName("Should handle pagination correctly")
    void findAll_ShouldHandlePagination() {
        // Given
        for (int i = 1; i <= 25; i++) {
            Formation formation = FormationTestDataBuilder.aFormation()
                    .withLibelle("Formation " + i)
                    .build();
            formationRepository.save(formation);
        }

        // When
        Pageable firstPage = PageRequest.of(0, 10);
        Pageable secondPage = PageRequest.of(1, 10);
        Pageable thirdPage = PageRequest.of(2, 10);

        Page<Formation> page1 = formationRepository.findAll(firstPage);
        Page<Formation> page2 = formationRepository.findAll(secondPage);
        Page<Formation> page3 = formationRepository.findAll(thirdPage);

        // Then
        FormationTestHelper.assertPageProperties(page1, 10, 3, true, false);
        FormationTestHelper.assertPageContent(page1, 10);

        FormationTestHelper.assertPageProperties(page2, 10, 3, false, false);
        FormationTestHelper.assertPageContent(page2, 10);

        FormationTestHelper.assertPageProperties(page3, 5, 3, false, true);
        FormationTestHelper.assertPageContent(page3, 5);

        assertThat(page1.getTotalElements()).isEqualTo(25);
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
}