package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.infrastructure.persistence.projection.FormationProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for FormationRepository using Testcontainers.
 */
@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("FormationRepository Integration Tests")
class FormationRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private FormationRepositoryImpl formationRepository;

    @Autowired
    private JpaFormationRepository jpaFormationRepository;

    private Formation formationPresentiel;
    private Formation formationEnLigne;
    private Formation formationHybride;

    @BeforeEach
    void setUp() {
        jpaFormationRepository.deleteAll();

        formationPresentiel = Formation.builder()
                .libelle("Formation Java")
                .formateurs("Expert Java")
                .description("Formation complète sur Java")
                .dateFormation(LocalDate.now().plusDays(10))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("Informatique")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Centre de formation")
                .ville("Paris")
                .build();

        formationEnLigne = Formation.builder()
                .libelle("Formation Python")
                .formateurs("Expert Python")
                .description("Formation Python avancée")
                .dateFormation(LocalDate.now().plusDays(20))
                .heureDebut(LocalTime.of(10, 0))
                .heureFin(LocalTime.of(16, 0))
                .secteur("Informatique")
                .region("Auvergne-Rhône-Alpes")
                .modalite(ModaliteFormation.EN_LIGNE)
                .nbParticipants(30)
                .lienParticipation("https://python.example.com")
                .build();

        formationHybride = Formation.builder()
                .libelle("Formation React")
                .formateurs("Expert Frontend")
                .description("Formation React et Redux")
                .dateFormation(LocalDate.now().plusDays(15))
                .heureDebut(LocalTime.of(9, 30))
                .heureFin(LocalTime.of(17, 30))
                .secteur("Informatique")
                .region("Nouvelle-Aquitaine")
                .modalite(ModaliteFormation.HYBRIDE)
                .nbParticipants(15)
                .lieu("Campus")
                .ville("Bordeaux")
                .lienParticipation("https://react.example.com")
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve formation")
    void save_ShouldPersistFormation() {
        // When
        Formation savedFormation = formationRepository.save(formationPresentiel);

        // Then
        assertThat(savedFormation.getId()).isNotNull();
        assertThat(savedFormation.getLibelle()).isEqualTo("Formation Java");

        Optional<Formation> retrieved = formationRepository.findById(savedFormation.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLibelle()).isEqualTo("Formation Java");
    }

    @Test
    @DisplayName("Should find formations by secteur")
    void findBySecteur_ShouldReturnFilteredResults() {
        // Given
        Formation marketingFormation = formationPresentiel.toBuilder()
                .secteur("Marketing")
                .libelle("Formation Marketing")
                .build();

        formationRepository.save(formationPresentiel);
        formationRepository.save(formationEnLigne);
        formationRepository.save(marketingFormation);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> itFormations = formationRepository.findBySecteur("Informatique", pageable);
        Page<Formation> marketingFormations = formationRepository.findBySecteur("Marketing", pageable);

        // Then
        assertThat(itFormations.getContent()).hasSize(2);
        assertThat(itFormations.getContent())
                .extracting(Formation::getSecteur)
                .containsOnly("Informatique");

        assertThat(marketingFormations.getContent()).hasSize(1);
        assertThat(marketingFormations.getContent().get(0).getSecteur()).isEqualTo("Marketing");
    }

    @Test
    @DisplayName("Should find formations by region")
    void findByRegion_ShouldReturnFilteredResults() {
        // Given
        formationRepository.save(formationPresentiel); // Île-de-France
        formationRepository.save(formationEnLigne); // Auvergne-Rhône-Alpes
        formationRepository.save(formationHybride); // Nouvelle-Aquitaine

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> idfFormations = formationRepository.findByRegion("Île-de-France", pageable);
        Page<Formation> araFormations = formationRepository.findByRegion("Auvergne-Rhône-Alpes", pageable);

        // Then
        assertThat(idfFormations.getContent()).hasSize(1);
        assertThat(idfFormations.getContent().get(0).getRegion()).isEqualTo("Île-de-France");

        assertThat(araFormations.getContent()).hasSize(1);
        assertThat(araFormations.getContent().get(0).getRegion()).isEqualTo("Auvergne-Rhône-Alpes");
    }

    @Test
    @DisplayName("Should find formations by modalite")
    void findByModalite_ShouldReturnFilteredResults() {
        // Given
        formationRepository.save(formationPresentiel);
        formationRepository.save(formationEnLigne);
        formationRepository.save(formationHybride);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> presentielFormations = formationRepository.findByModalite(ModaliteFormation.PRESENTIEL, pageable);
        Page<Formation> enLigneFormations = formationRepository.findByModalite(ModaliteFormation.EN_LIGNE, pageable);
        Page<Formation> hybrideFormations = formationRepository.findByModalite(ModaliteFormation.HYBRIDE, pageable);

        // Then
        assertThat(presentielFormations.getContent()).hasSize(1);
        assertThat(presentielFormations.getContent().get(0).getModalite()).isEqualTo(ModaliteFormation.PRESENTIEL);

        assertThat(enLigneFormations.getContent()).hasSize(1);
        assertThat(enLigneFormations.getContent().get(0).getModalite()).isEqualTo(ModaliteFormation.EN_LIGNE);

        assertThat(hybrideFormations.getContent()).hasSize(1);
        assertThat(hybrideFormations.getContent().get(0).getModalite()).isEqualTo(ModaliteFormation.HYBRIDE);
    }

    @Test
    @DisplayName("Should find formations by date range")
    void findByDateFormationBetween_ShouldReturnFilteredResults() {
        // Given
        formationRepository.save(formationPresentiel); // +10 days
        formationRepository.save(formationEnLigne); // +20 days
        formationRepository.save(formationHybride); // +15 days

        LocalDate startDate = LocalDate.now().plusDays(12);
        LocalDate endDate = LocalDate.now().plusDays(25);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> formationsInRange = formationRepository.findByDateFormationBetween(startDate, endDate, pageable);

        // Then
        assertThat(formationsInRange.getContent()).hasSize(2);
        assertThat(formationsInRange.getContent())
                .extracting(Formation::getLibelle)
                .containsExactlyInAnyOrder("Formation Python", "Formation React");
    }

    @Test
    @DisplayName("Should filter by complex criteria")
    void findByFilters_ShouldApplyMultipleFilters() {
        // Given
        Formation formation1 = formationPresentiel.toBuilder()
                .secteur("Informatique")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .build();

        Formation formation2 = formationEnLigne.toBuilder()
                .secteur("Informatique")
                .region("Île-de-France")
                .modalite(ModaliteFormation.EN_LIGNE)
                .build();

        Formation formation3 = formationHybride.toBuilder()
                .secteur("Marketing")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .build();

        formationRepository.save(formation1);
        formationRepository.save(formation2);
        formationRepository.save(formation3);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> result = formationRepository.findByFilters(
                "Informatique", "Île-de-France", ModaliteFormation.PRESENTIEL, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSecteur()).isEqualTo("Informatique");
        assertThat(result.getContent().get(0).getRegion()).isEqualTo("Île-de-France");
        assertThat(result.getContent().get(0).getModalite()).isEqualTo(ModaliteFormation.PRESENTIEL);
    }

    @Test
    @DisplayName("Should filter by statut using application logic")
    void findByFilters_ShouldFilterByStatut() {
        // Given
        Formation formationPast = formationPresentiel.toBuilder()
                .dateFormation(LocalDate.now().minusDays(1))
                .build();

        Formation formationFuture = formationEnLigne.toBuilder()
                .dateFormation(LocalDate.now().plusDays(30))
                .build();

        formationRepository.save(formationPast);
        formationRepository.save(formationFuture);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Formation> futureFormations = formationRepository.findByFilters(
                null, null, null, FormationStatut.A_VENIR, pageable);

        Page<Formation> pastFormations = formationRepository.findByFilters(
                null, null, null, FormationStatut.TERMINEE, pageable);

        // Then
        assertThat(futureFormations.getContent()).hasSize(1);
        assertThat(futureFormations.getContent().get(0).getStatut()).isEqualTo(FormationStatut.A_VENIR);

        assertThat(pastFormations.getContent()).hasSize(1);
        assertThat(pastFormations.getContent().get(0).getStatut()).isEqualTo(FormationStatut.TERMINEE);
    }

    @Test
    @DisplayName("Should work with projections")
    void findProjectionById_ShouldReturnProjection() {
        // Given
        Formation savedFormation = formationRepository.save(formationPresentiel);

        // When
        Optional<FormationProjection> projection = formationRepository.findProjectionById(savedFormation.getId());

        // Then
        assertThat(projection).isPresent();
        assertThat(projection.get().getId()).isEqualTo(savedFormation.getId());
        assertThat(projection.get().getLibelle()).isEqualTo("Formation Java");
        assertThat(projection.get().getModalite()).isEqualTo(ModaliteFormation.PRESENTIEL);
    }

    @Test
    @DisplayName("Should return all projections")
    void findAllProjections_ShouldReturnAllProjections() {
        // Given
        formationRepository.save(formationPresentiel);
        formationRepository.save(formationEnLigne);
        formationRepository.save(formationHybride);

        // When
        List<FormationProjection> projections = formationRepository.findAllProjections();

        // Then
        assertThat(projections).hasSize(3);
        assertThat(projections)
                .extracting(FormationProjection::getLibelle)
                .containsExactlyInAnyOrder("Formation Java", "Formation Python", "Formation React");
    }

    @Test
    @DisplayName("Should find projections by filters")
    void findProjectionsByFilters_ShouldReturnFilteredProjections() {
        // Given
        formationRepository.save(formationPresentiel);
        formationRepository.save(formationEnLigne);
        formationRepository.save(formationHybride);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<FormationProjection> projections = formationRepository.findProjectionsByFilters(
                "Informatique", null, null, pageable);

        // Then
        assertThat(projections.getContent()).hasSize(3);
        assertThat(projections.getContent())
                .extracting(FormationProjection::getSecteur)
                .containsOnly("Informatique");
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void findAll_ShouldHandlePagination() {
        // Given
        for (int i = 1; i <= 25; i++) {
            Formation formation = formationPresentiel.toBuilder()
                    .libelle("Formation " + i)
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
        assertThat(page1.getTotalElements()).isEqualTo(25);
        assertThat(page1.getTotalPages()).isEqualTo(3);
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page1.isFirst()).isTrue();

        assertThat(page2.getContent()).hasSize(10);
        assertThat(page2.isFirst()).isFalse();
        assertThat(page2.isLast()).isFalse();

        assertThat(page3.getContent()).hasSize(5);
        assertThat(page3.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should validate formation data before saving")
    void save_ShouldValidateFormationData() {
        // Given - Formation with invalid time range
        Formation invalidFormation = formationPresentiel.toBuilder()
                .heureDebut(LocalTime.of(17, 0))
                .heureFin(LocalTime.of(9, 0))
                .build();

        // When & Then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> formationRepository.save(invalidFormation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heure de fin doit être postérieure");
    }

    @Test
    @DisplayName("Should delete formation correctly")
    void deleteById_ShouldRemoveFormation() {
        // Given
        Formation savedFormation = formationRepository.save(formationPresentiel);

        // When
        formationRepository.deleteById(savedFormation.getId());

        // Then
        Optional<Formation> deleted = formationRepository.findById(savedFormation.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Should check formation existence")
    void existsById_ShouldReturnCorrectValue() {
        // Given
        Formation savedFormation = formationRepository.save(formationPresentiel);

        // When & Then
        assertThat(formationRepository.existsById(savedFormation.getId())).isTrue();
        assertThat(formationRepository.existsById(java.util.UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("Should count formations correctly")
    void count_ShouldReturnCorrectCount() {
        // Given
        formationRepository.save(formationPresentiel);
        formationRepository.save(formationEnLigne);

        // When & Then
        assertThat(formationRepository.count()).isEqualTo(2);
    }
}