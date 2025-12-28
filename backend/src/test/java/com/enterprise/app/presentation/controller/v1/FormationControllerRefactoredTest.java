package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.CreateFormationRequest;
import com.enterprise.app.application.dto.FormationDTO;
import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.testing.config.BaseWebMvcTest;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import com.enterprise.app.testing.builders.FormationTestDataBuilder;
import com.enterprise.app.testing.mocks.FormationServiceMockProvider;
import com.enterprise.app.testing.mocks.FormationMapperMockProvider;
import com.enterprise.app.testing.helpers.FormationTestHelper;
import com.enterprise.app.testing.helpers.MockMvcTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test refactorisé du FormationController utilisant l'architecture centralisée des mocks.

@DisplayName("Formation Controller - Tests Refactorisés")
 */
class FormationControllerRefactoredTest extends BaseWebMvcTest {
/*
    private MockMvcTestHelper mockMvcHelper;
    private Long formationId;
    private Formation formation;
    private FormationDTO formationDTO;
    private FormationProjection formationProjection;

    @BeforeEach
    void setUp() {
        mockMvcHelper = new MockMvcTestHelper(mockMvc, objectMapper);
        formationId = FormationFixtures.FORMATION_ID_1;

        formation = FormationFixtures.defaultFormationPresentiel();

        formationDTO = FormationDTO.builder()
                .id(formationId)
                .libelle(FormationFixtures.FORMATION_LIBELLE_JAVA)
                .formateurs(FormationFixtures.FORMATEUR_JEAN_DUPONT)
                .description("Formation approfondie sur Java")
                .dateFormation(LocalDate.now().plusDays(15))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("Informatique")
                .region("Île-de-France")
                .modalite(com.enterprise.app.domain.model.ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Salle A")
                .ville("Paris")
                .statut(com.enterprise.app.domain.model.FormationStatut.A_VENIR)
                .nbParticipantsInscrits(5)
                .complet(false)
                .build();

        formationProjection = createMockProjection();
    }

    @Test
    @WithMockUser
    @DisplayName("Should return formations with pagination")
    void getAllFormations_ShouldReturnFormations() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        var projectionPage = new PageImpl<>(Arrays.asList(formationProjection), pageable, 1);

        when(formationService.searchFormationProjections(eq(1L), eq(1L),
                any(), any(), any(Pageable.class)))
                .thenReturn(projectionPage);
        when(formationMapper.toDTO(formationProjection)).thenReturn(formationDTO);

        // When & Then
        mockMvcHelper.performGetWithParams(API_V1_FORMATIONS, "secteur", "Informatique")
                .andExpect(status().isOk())
                .andExpectAll(MockMvcTestHelper.expectPageResponse(1, 1, 1))
                .andExpectAll(FormationTestHelper.standardFormationDTOMatchers(formationDTO));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return formation by ID when it exists")
    void getFormationById_ShouldReturnFormation_WhenExists() throws Exception {
        // Given
        when(formationService.getFormationProjectionById(formationId)).thenReturn(formationProjection);
        when(formationMapper.toDTO(formationProjection)).thenReturn(formationDTO);

        // When & Then
        mockMvcHelper.performGetAndExpectOk(API_V1_FORMATIONS + "/{id}", formationId)
                .andExpectAll(FormationTestHelper.standardFormationDTOMatchers(formationDTO));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should create formation when request is valid")
    void createFormation_ShouldCreateFormation_WhenValidRequest() throws Exception {
        // Given
        CreateFormationRequest createRequest = CreateFormationRequest.builder()
                .libelle(FormationFixtures.FORMATION_LIBELLE_JAVA)
                .formateurs(FormationFixtures.FORMATEUR_JEAN_DUPONT)
                .description("Formation approfondie sur Java")
                .dateFormation(LocalDate.now().plusDays(15))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("Informatique")
                .region("Île-de-France")
                .modalite(com.enterprise.app.domain.model.ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Salle A")
                .ville("Paris")
                .build();

        when(formationMapper.toEntity(any(CreateFormationRequest.class))).thenReturn(formation);
        when(formationService.createFormation(any(Formation.class))).thenReturn(formation);
        when(formationMapper.toDTO(any(Formation.class))).thenReturn(formationDTO);

        // When & Then
        mockMvcHelper.performPostAndExpectCreated(API_V1_FORMATIONS, createRequest)
                .andExpectAll(FormationTestHelper.standardFormationDTOMatchers(formationDTO));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return forbidden when user tries to create formation")
    void createFormation_ShouldReturnForbidden_WhenUserRole() throws Exception {
        // Given
        CreateFormationRequest createRequest = CreateFormationRequest.builder()
                .libelle("Formation Test")
                .build();

        // When & Then
        mockMvcHelper.performAndExpectForbidden("POST", API_V1_FORMATIONS, createRequest);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Should update formation when request is valid")
    void updateFormation_ShouldUpdateFormation_WhenValidRequest() throws Exception {
        // Given
        var updateRequest = com.enterprise.app.application.dto.UpdateFormationRequest.builder()
                .libelle("Formation Updated")
                .formateurs("Formateur Updated")
                .description("Description updated")
                .dateFormation(LocalDate.now().plusDays(20))
                .heureDebut(LocalTime.of(10, 0))
                .heureFin(LocalTime.of(18, 0))
                .secteur("Informatique")
                .region("Provence-Alpes-Côte d'Azur")
                .modalite(ModaliteFormation.EN_LIGNE)
                .nbParticipants(15)
                .lieu("Salle B")
                .ville("Marseille")
                .build();

        Formation updatedFormation = FormationTestDataBuilder.aFormation()
                .withId(formationId)
                .withLibelle("Formation Updated")
                .build();

        FormationDTO updatedDTO = formationDTO.toBuilder()
                .libelle("Formation Updated")
                .build();

        when(formationService.getFormationById(formationId)).thenReturn(formation);
        when(formationService.updateFormation(eq(formationId), any(Formation.class))).thenReturn(updatedFormation);
        when(formationMapper.toDTO(updatedFormation)).thenReturn(updatedDTO);

        // When & Then
        mockMvcHelper.performPutAndExpectOk(API_V1_FORMATIONS + "/{id}", updateRequest, formationId)
                .andExpect(MockMvcTestHelper.expectJsonField("$.libelle", "Formation Updated"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should delete formation successfully")
    void deleteFormation_ShouldDeleteFormation() throws Exception {
        // Given - Le service mock ne fait rien par défaut pour delete

        // When & Then
        mockMvcHelper.performDeleteAndExpectNoContent(API_V1_FORMATIONS + "/{id}", formationId);
    }

    @Test
    @WithMockUser
    @DisplayName("Should return bad request when invalid data")
    void createFormation_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        // Given
        CreateFormationRequest invalidRequest = CreateFormationRequest.builder()
                .libelle("") // Libellé vide
                .build();

        // When & Then
        mockMvcHelper.performPostAndExpectBadRequest(API_V1_FORMATIONS, invalidRequest);
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle empty results gracefully")
    void getAllFormations_ShouldReturnEmptyPage_WhenNoResults() throws Exception {
        // Given
        var emptyPage = new PageImpl<FormationProjection>(Arrays.asList());
        when(formationService.searchFormationProjections(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When & Then
        mockMvcHelper.performGetAndExpectOk(API_V1_FORMATIONS)
                .andExpectAll(MockMvcTestHelper.expectPageResponse(0, 0, 0));
    }
*/
    /**
     * Crée un mock de FormationProjection pour les tests.
     */
    /*
    private FormationProjection createMockProjection() {
        return new FormationProjection() {
            @Override public Long getId() { return formationId; }
            @Override public String getLibelle() { return FormationFixtures.FORMATION_LIBELLE_JAVA; }
            @Override public String getFormateurs() { return FormationFixtures.FORMATEUR_JEAN_DUPONT; }
            @Override public String getDescription() { return "Formation approfondie sur Java"; }
            @Override public LocalDate getDateFormation() { return LocalDate.now().plusDays(15); }
            @Override public LocalTime getHeureDebut() { return LocalTime.of(9, 0); }
            @Override public LocalTime getHeureFin() { return LocalTime.of(17, 0); }
            @Override public String getSecteur() { return "Informatique"; }
            @Override public String getRegion() { return "Île-de-France"; }
            @Override public com.enterprise.app.domain.model.ModaliteFormation getModalite() {
                return com.enterprise.app.domain.model.ModaliteFormation.PRESENTIEL;
            }
            @Override public Integer getNbParticipants() { return 20; }
            @Override public String getLieu() { return "Salle A"; }
            @Override public String getVille() { return "Paris"; }
            @Override public String getLienParticipation() { return null; }
            @Override public com.enterprise.app.domain.model.FormationStatut getStatut() {
                return com.enterprise.app.domain.model.FormationStatut.A_VENIR;
            }
            @Override public java.time.LocalDateTime getCreatedAt() { return java.time.LocalDateTime.now(); }
            @Override public java.time.LocalDateTime getUpdatedAt() { return java.time.LocalDateTime.now(); }
            @Override public Integer getNbParticipantsInscrits() { return 5; }
        };
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 'Tirage au sort' formations by secteur")
    void getAllFormations_ShouldReturnTirageAuSortFormations() throws Exception {
        // Given

        FormationDTO tirageAuSortDTO = FormationDTO.builder()
                .id(4L)
                .libelle("Tirage au sort")
                .formateurs("LAGRACE Elodie - DUPONT Frédérique")
                .secteur("Regime général")
                .region("IDF")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(30)
                .lieu("1 rue serpentine Courbevoie")
                .ville("Courbevoie")
                .build();

        FormationProjection tirageAuSortProjection = createTirageAuSortProjection();

        Pageable pageable = PageRequest.of(0, 20);
        var projectionPage = new PageImpl<>(Arrays.asList(tirageAuSortProjection), pageable, 1);

        when(formationService.searchFormationProjections(eq(2L), any(),
                any(), any(), any(Pageable.class)))
                .thenReturn(projectionPage);
        when(formationMapper.toDTO(tirageAuSortProjection)).thenReturn(tirageAuSortDTO);

        // When & Then
        mockMvcHelper.performGetWithParams(API_V1_FORMATIONS, "secteur", "Regime général")
                .andExpect(status().isOk())
                .andExpectAll(MockMvcTestHelper.expectPageResponse(1, 1, 1))
                .andExpect(MockMvcTestHelper.expectJsonField("$.content[0].libelle", "Tirage au sort"))
                .andExpect(MockMvcTestHelper.expectJsonField("$.content[0].formateurs", "LAGRACE Elodie - DUPONT Frédérique"))
                .andExpect(MockMvcTestHelper.expectJsonField("$.content[0].secteur", "Regime général"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return formation in Martinique region")
    void getFormationById_ShouldReturnFormationMartinique() throws Exception {
        // Given
        Long martiniqueFormationId = 7L;

        FormationDTO martiniqueDTO = FormationDTO.builder()
                .id(martiniqueFormationId)
                .libelle("Tirage au sort - Sécrétaire")
                .formateurs("LAGRACE Elodie - DUPONT Frédérique")
                .secteur("Regime général")
                .region("Martinique")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(30)
                .lieu("1 place du Fort")
                .ville("Fort de France")
                .build();

        FormationProjection martiniqueProjection = createMartiniqueProjection();

        when(formationService.getFormationProjectionById(martiniqueFormationId)).thenReturn(martiniqueProjection);
        when(formationMapper.toDTO(martiniqueProjection)).thenReturn(martiniqueDTO);

        // When & Then
        mockMvcHelper.performGetAndExpectOk(API_V1_FORMATIONS + "/{id}", martiniqueFormationId)
                .andExpect(MockMvcTestHelper.expectJsonField("$.libelle", "Tirage au sort - Sécrétaire"))
                .andExpect(MockMvcTestHelper.expectJsonField("$.region", "Martinique"))
                .andExpect(MockMvcTestHelper.expectJsonField("$.ville", "Fort de France"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should create 'Tirage au sort' formation successfully")
    void createFormation_ShouldCreateTirageAuSort() throws Exception {
        // Given
        CreateFormationRequest createTirageAuSort = CreateFormationRequest.builder()
                .libelle("Tirage au sort")
                .formateurs("LAGRACE Elodie - DUPONT Frédérique")
                .description("Tirage au sort - Régime général")
                .dateFormation(LocalDate.of(2026, 1, 15))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(15, 0))
                .secteur("Regime général")
                .region("IDF")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(30)
                .lieu("1 rue serpentine Courbevoie")
                .ville("Courbevoie")
                .build();

        Formation tirageAuSort = FormationFixtures.tirageAuSortPresentielRG();
        FormationDTO tirageAuSortDTO = FormationDTO.builder()
                .id(4L)
                .libelle("Tirage au sort")
                .formateurs("LAGRACE Elodie - DUPONT Frédérique")
                .secteur("Regime général")
                .region("IDF")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(30)
                .lieu("1 rue serpentine Courbevoie")
                .ville("Courbevoie")
                .build();

        when(formationMapper.toEntity(any(CreateFormationRequest.class))).thenReturn(tirageAuSort);
        when(formationService.createFormation(any(Formation.class))).thenReturn(tirageAuSort);
        when(formationMapper.toDTO(any(Formation.class))).thenReturn(tirageAuSortDTO);

        // When & Then
        mockMvcHelper.performPostAndExpectCreated(API_V1_FORMATIONS, createTirageAuSort)
                .andExpect(MockMvcTestHelper.expectJsonField("$.libelle", "Tirage au sort"))
                .andExpect(MockMvcTestHelper.expectJsonField("$.formateurs", "LAGRACE Elodie - DUPONT Frédérique"))
                .andExpect(MockMvcTestHelper.expectJsonField("$.nbParticipants", 30));
    }

    @Test
    @WithMockUser
    @DisplayName("Should filter formations by MSA secteur")
    void getAllFormations_ShouldFilterByMSASecteur() throws Exception {
        // Given
        FormationProjection msaProjection = createMSAProjection();
        FormationDTO msaDTO = FormationDTO.builder()
                .id(6L)
                .libelle("Tirage au sort - RU")
                .formateurs("LAGRACE Elodie - DUPONT Frédérique")
                .secteur("MSA")
                .region("IDF")
                .modalite(ModaliteFormation.PRESENTIEL)
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        var projectionPage = new PageImpl<>(Arrays.asList(msaProjection), pageable, 1);

        when(formationService.searchFormationProjections(eq(3L), any(),
                any(), any(), any(Pageable.class)))
                .thenReturn(projectionPage);
        when(formationMapper.toDTO(msaProjection)).thenReturn(msaDTO);

        // When & Then
        mockMvcHelper.performGetWithParams(API_V1_FORMATIONS, "secteur", "MSA")
                .andExpect(status().isOk())
                .andExpectAll(MockMvcTestHelper.expectPageResponse(1, 1, 1))
                .andExpect(MockMvcTestHelper.expectJsonField("$.content[0].secteur", "MSA"))
                .andExpect(MockMvcTestHelper.expectJsonField("$.content[0].libelle", "Tirage au sort - RU"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle online formation with participation link")
    void getAllFormations_ShouldReturnOnlineFormationWithLink() throws Exception {
        // Given
        FormationProjection onlineProjection = createOnlineProjection();
        FormationDTO onlineDTO = FormationDTO.builder()
                .id(5L)
                .libelle("Tirage au sort")
                .formateurs("LAGRACE Elodie - DUPONT Frédérique")
                .secteur("Regime général")
                .region("IDF")
                .modalite(ModaliteFormation.EN_LIGNE)
                .nbParticipants(30)
                .lienParticipation("https://localhost")
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        var projectionPage = new PageImpl<>(Arrays.asList(onlineProjection), pageable, 1);

        when(formationService.searchFormationProjections(any(), any(),
                eq(ModaliteFormation.EN_LIGNE), any(), any(Pageable.class)))
                .thenReturn(projectionPage);
        when(formationMapper.toDTO(onlineProjection)).thenReturn(onlineDTO);

        // When & Then
        mockMvcHelper.performGetWithParams(API_V1_FORMATIONS, "modalite", "EN_LIGNE")
                .andExpect(status().isOk())
                .andExpectAll(MockMvcTestHelper.expectPageResponse(1, 1, 1))
                .andExpect(MockMvcTestHelper.expectJsonField("$.content[0].modalite", "EN_LIGNE"))
                .andExpect(MockMvcTestHelper.expectJsonField("$.content[0].lienParticipation", "https://localhost"));
    }

    private FormationProjection createTirageAuSortProjection() {
        return new FormationProjection() {
            @Override public Long getId() { return 4L; }
            @Override public String getLibelle() { return "Tirage au sort"; }
            @Override public String getFormateurs() { return "LAGRACE Elodie - DUPONT Frédérique"; }
            @Override public String getDescription() { return "Tirage au sort - Régime général"; }
            @Override public LocalDate getDateFormation() { return LocalDate.of(2026, 1, 15); }
            @Override public LocalTime getHeureDebut() { return LocalTime.of(9, 0); }
            @Override public LocalTime getHeureFin() { return LocalTime.of(15, 0); }
            @Override public com.enterprise.app.domain.model.Secteur getSecteur() { return FormationFixtures.SECTEUR_RG; }
            @Override public com.enterprise.app.domain.model.Region getRegion() { return FormationFixtures.REGION_IDF; }
            @Override public ModaliteFormation getModalite() { return ModaliteFormation.PRESENTIEL; }
            @Override public Integer getNbParticipants() { return 30; }
            @Override public String getLieu() { return "1 rue serpentine Courbevoie"; }
            @Override public String getVille() { return "Courbevoie"; }
            @Override public String getLienParticipation() { return null; }
            @Override public com.enterprise.app.domain.model.FormationStatut getStatut() {
                return com.enterprise.app.domain.model.FormationStatut.A_VENIR;
            }
            @Override public java.time.LocalDateTime getCreatedAt() { return java.time.LocalDateTime.now(); }
            @Override public java.time.LocalDateTime getUpdatedAt() { return java.time.LocalDateTime.now(); }
            @Override public Integer getNbParticipantsInscrits() { return 0; }
        };
    }

    private FormationProjection createMartiniqueProjection() {
        return new FormationProjection() {
            @Override public Long getId() { return 7L; }
            @Override public String getLibelle() { return "Tirage au sort - Sécrétaire"; }
            @Override public String getFormateurs() { return "LAGRACE Elodie - DUPONT Frédérique"; }
            @Override public String getDescription() { return "Tirage au sort - Régime général"; }
            @Override public LocalDate getDateFormation() { return LocalDate.of(2026, 3, 20); }
            @Override public LocalTime getHeureDebut() { return LocalTime.of(9, 0); }
            @Override public LocalTime getHeureFin() { return LocalTime.of(15, 0); }
            @Override public com.enterprise.app.domain.model.Secteur getSecteur() { return FormationFixtures.SECTEUR_RG; }
            @Override public String getRegion() { return "Martinique"; }
            @Override public ModaliteFormation getModalite() { return ModaliteFormation.PRESENTIEL; }
            @Override public Integer getNbParticipants() { return 30; }
            @Override public String getLieu() { return "1 place du Fort"; }
            @Override public String getVille() { return "Fort de France"; }
            @Override public String getLienParticipation() { return null; }
            @Override public com.enterprise.app.domain.model.FormationStatut getStatut() {
                return com.enterprise.app.domain.model.FormationStatut.A_VENIR;
            }
            @Override public java.time.LocalDateTime getCreatedAt() { return java.time.LocalDateTime.now(); }
            @Override public java.time.LocalDateTime getUpdatedAt() { return java.time.LocalDateTime.now(); }
            @Override public Integer getNbParticipantsInscrits() { return 0; }
        };
    }

    private FormationProjection createMSAProjection() {
        return new FormationProjection() {
            @Override public Long getId() { return 6L; }
            @Override public String getLibelle() { return "Tirage au sort - RU"; }
            @Override public String getFormateurs() { return "LAGRACE Elodie - DUPONT Frédérique"; }
            @Override public String getDescription() { return "Tirage au sort - Régime général"; }
            @Override public LocalDate getDateFormation() { return LocalDate.of(2026, 1, 15); }
            @Override public LocalTime getHeureDebut() { return LocalTime.of(9, 0); }
            @Override public LocalTime getHeureFin() { return LocalTime.of(15, 0); }
            @Override public com.enterprise.app.domain.model.Secteur getSecteur() { return FormationFixtures.SECTEUR_MSA; }
            @Override public com.enterprise.app.domain.model.Region getRegion() { return FormationFixtures.REGION_IDF; }
            @Override public ModaliteFormation getModalite() { return ModaliteFormation.PRESENTIEL; }
            @Override public Integer getNbParticipants() { return 30; }
            @Override public String getLieu() { return "1 rue serpentine Courbevoie"; }
            @Override public String getVille() { return "Courbevoie"; }
            @Override public String getLienParticipation() { return null; }
            @Override public com.enterprise.app.domain.model.FormationStatut getStatut() {
                return com.enterprise.app.domain.model.FormationStatut.A_VENIR;
            }
            @Override public java.time.LocalDateTime getCreatedAt() { return java.time.LocalDateTime.now(); }
            @Override public java.time.LocalDateTime getUpdatedAt() { return java.time.LocalDateTime.now(); }
            @Override public Integer getNbParticipantsInscrits() { return 0; }
        };
    }

    private FormationProjection createOnlineProjection() {
        return new FormationProjection() {
            @Override public Long getId() { return 5L; }
            @Override public String getLibelle() { return "Tirage au sort"; }
            @Override public String getFormateurs() { return "LAGRACE Elodie - DUPONT Frédérique"; }
            @Override public String getDescription() { return "Tirage au sort - Régime général"; }
            @Override public LocalDate getDateFormation() { return LocalDate.of(2026, 1, 15); }
            @Override public LocalTime getHeureDebut() { return LocalTime.of(9, 0); }
            @Override public LocalTime getHeureFin() { return LocalTime.of(15, 0); }
            @Override public com.enterprise.app.domain.model.Secteur getSecteur() { return FormationFixtures.SECTEUR_RG; }
            @Override public com.enterprise.app.domain.model.Region getRegion() { return FormationFixtures.REGION_IDF; }
            @Override public ModaliteFormation getModalite() { return ModaliteFormation.EN_LIGNE; }
            @Override public Integer getNbParticipants() { return 30; }
            @Override public String getLieu() { return null; }
            @Override public String getVille() { return null; }
            @Override public String getLienParticipation() { return "https://localhost"; }
            @Override public com.enterprise.app.domain.model.FormationStatut getStatut() {
                return com.enterprise.app.domain.model.FormationStatut.A_VENIR;
            }
            @Override public java.time.LocalDateTime getCreatedAt() { return java.time.LocalDateTime.now(); }
            @Override public java.time.LocalDateTime getUpdatedAt() { return java.time.LocalDateTime.now(); }
            @Override public Integer getNbParticipantsInscrits() { return 0; }
        };
    }

     */
}