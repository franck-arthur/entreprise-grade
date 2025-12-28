package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.CreateFormationRequest;
import com.enterprise.app.application.dto.UpdateFormationRequest;
import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.infrastructure.persistence.JpaFormationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;

import static com.enterprise.app.domain.model.ModaliteFormation.EN_LIGNE;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FormationController using Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("FormationController Integration Tests")
class FormationControllerIntegrationTest {

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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaFormationRepository formationRepository;

    private Formation formation;
    private CreateFormationRequest createRequest;

    @BeforeEach
    void setUp() {
        formationRepository.deleteAll();

        formation = Formation.builder()
                .libelle("Formation Spring Boot")
                .formateurs("Expert Spring")
                .description("Formation complète sur Spring Boot")
                .dateFormation(LocalDate.now().plusDays(30))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur(FormationFixtures.SECTEUR_MSA)
                .region(FormationFixtures.REGION_IDF)
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Centre de formation")
                .ville("Paris")
                .build();

        createRequest = CreateFormationRequest.builder()
                .libelle("Formation React")
                .formateurs("Expert Frontend")
                .description("Formation sur React et Redux")
                .dateFormation(LocalDate.now().plusDays(15))
                .heureDebut(LocalTime.of(10, 0))
                .heureFin(LocalTime.of(18, 0))
                .secteurId(FormationFixtures.SECTEUR_MSA.getId())
                .regionId(FormationFixtures.REGION_AURA.getId())
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(15)
                .lieu("Campus universitaire")
                .ville("Lyon")
                .build();
    }

    @Test
    @WithMockUser
    @DisplayName("Should retrieve all formations with pagination")
    void getAllFormations_ShouldReturnFormationsWithPagination() throws Exception {
        // Given
        Formation savedFormation = formationRepository.save(formation);

        // When & Then
        mockMvc.perform(get("/api/v1/formations")
                .param("size", "10")
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].libelle").value("Formation Spring Boot"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("Should filter formations by secteur")
    void getAllFormations_ShouldFilterBySecteur() throws Exception {
        // Given
        Formation formation1 = formation.toBuilder().secteur(FormationFixtures.SECTEUR_MSA).build();
        Formation formation2 = formation.toBuilder().secteur(FormationFixtures.SECTEUR_RG).libelle("Formation Marketing").build();

        formationRepository.save(formation1);
        formationRepository.save(formation2);

        // When & Then
        mockMvc.perform(get("/api/v1/formations")
                .param("secteurId", FormationFixtures.SECTEUR_MSA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].secteur.nom").value("Informatique"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should filter formations by region")
    void getAllFormations_ShouldFilterByRegion() throws Exception {
        // Given
        Formation formation1 = formation.toBuilder().region(FormationFixtures.REGION_IDF).build();
        Formation formation2 = formation.toBuilder().region(FormationFixtures.REGION_AURA).libelle("Formation Lyon").build();

        formationRepository.save(formation1);
        formationRepository.save(formation2);

        // When & Then
        mockMvc.perform(get("/api/v1/formations")
                .param("regionId", FormationFixtures.REGION_IDF.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].region.nom").value("Île-de-France"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should filter formations by modalite")
    void getAllFormations_ShouldFilterByModalite() throws Exception {
        // Given
        Formation formation1 = formation.toBuilder().modalite(ModaliteFormation.PRESENTIEL).build();
        Formation formation2 = formation.toBuilder()
                .modalite(EN_LIGNE)
                .libelle("Formation en ligne")
                .lieu(null)
                .ville(null)
                .lienParticipation("https://example.com")
                .build();

        formationRepository.save(formation1);
        formationRepository.save(formation2);

        // When & Then
        mockMvc.perform(get("/api/v1/formations")
                .param("modalite", "EN_LIGNE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].modalite").value("EN_LIGNE"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should retrieve formation by ID")
    void getFormationById_ShouldReturnFormation() throws Exception {
        // Given
        Formation savedFormation = formationRepository.save(formation);

        // When & Then
        mockMvc.perform(get("/api/v1/formations/{id}", savedFormation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedFormation.getId()))
                .andExpect(jsonPath("$.libelle").value("Formation Spring Boot"))
                .andExpect(jsonPath("$.formateurs").value("Expert Spring"))
                .andExpect(jsonPath("$.modalite").value("PRESENTIEL"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when formation not found")
    void getFormationById_ShouldReturn404_WhenNotFound() throws Exception {
        // Given
        Long nonExistentId = 999L;

        // When & Then
        mockMvc.perform(get("/api/v1/formations/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should create formation successfully")
    void createFormation_ShouldCreateFormation_WhenValidRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/formations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libelle").value("Formation React"))
                .andExpect(jsonPath("$.formateurs").value("Expert Frontend"))
                .andExpect(jsonPath("$.ville").value("Lyon"));

        // Verify in database
        long count = formationRepository.count();
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should validate formation data on creation")
    void createFormation_ShouldValidateFormationData() throws Exception {
        // Given - Invalid request with missing required fields
        CreateFormationRequest invalidRequest = CreateFormationRequest.builder()
                .libelle("") // Empty libelle
                .dateFormation(LocalDate.now().minusDays(1)) // Past date
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/formations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when user tries to create formation")
    void createFormation_ShouldReturn403_WhenUserRole() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/formations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Should update formation successfully")
    void updateFormation_ShouldUpdateFormation_WhenValidRequest() throws Exception {
        // Given
        Formation savedFormation = formationRepository.save(formation);

        UpdateFormationRequest updateRequest = UpdateFormationRequest.builder()
                .libelle("Formation Spring Boot Avancée")
                .formateurs("Expert Spring Senior")
                .description("Formation avancée sur Spring Boot")
                .dateFormation(LocalDate.now().plusDays(45))
                .heureDebut(LocalTime.of(8, 30))
                .heureFin(LocalTime.of(17, 30))
                .secteurId(FormationFixtures.SECTEUR_MSA.getId())
                .regionId(FormationFixtures.REGION_IDF.getId())
                .modalite(EN_LIGNE)
                .nbParticipants(25)
                .lieu("Nouveau centre")
                .ville("Paris")
                .lienParticipation("https://example.com/formation")
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/formations/{id}", savedFormation.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelle").value("Formation Spring Boot Avancée"))
                .andExpect(jsonPath("$.formateurs").value("Expert Spring Senior"))
                .andExpect(jsonPath("$.modalite").value(EN_LIGNE));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should delete formation successfully")
    void deleteFormation_ShouldDeleteFormation() throws Exception {
        // Given
        Formation savedFormation = formationRepository.save(formation);

        // When & Then
        mockMvc.perform(delete("/api/v1/formations/{id}", savedFormation.getId())
                .with(csrf()))
                .andExpect(status().isNoContent());

        // Verify deletion
        boolean exists = formationRepository.existsById(savedFormation.getId());
        org.assertj.core.api.Assertions.assertThat(exists).isFalse();
    }

    @Test
    @WithMockUser(roles = "TECH_LEAD")
    @DisplayName("Should retrieve formation participants")
    void getFormationParticipants_ShouldReturnParticipants() throws Exception {
        // Given
        Formation savedFormation = formationRepository.save(formation);

        // When & Then
        mockMvc.perform(get("/api/v1/formations/{id}/participants", savedFormation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when user tries to view participants")
    void getFormationParticipants_ShouldReturn403_WhenUserRole() throws Exception {
        // Given
        Formation savedFormation = formationRepository.save(formation);

        // When & Then
        mockMvc.perform(get("/api/v1/formations/{id}/participants", savedFormation.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle complex filtering scenarios")
    void getAllFormations_ShouldHandleComplexFiltering() throws Exception {
        // Given
        formationRepository.save(FormationFixtures.defaultFormationPresentiel());
        formationRepository.save(FormationFixtures.tirageAuSortRU());
        formationRepository.save(FormationFixtures.tirageAuSortEnLigneRG());

        // When & Then - Filter by secteur and region
        mockMvc.perform(get("/api/v1/formations")
                .param("secteurId", FormationFixtures.SECTEUR_MSA.getId().toString())
                .param("regionId", FormationFixtures.REGION_IDF.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].libelle").value("Formation Java"));
    }
}