package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.CreateFormationRequest;
import com.enterprise.app.application.dto.FormationDTO;
import com.enterprise.app.application.dto.UpdateFormationRequest;
import com.enterprise.app.application.dto.SecteurDTO;
import com.enterprise.app.application.dto.RegionDTO;
import com.enterprise.app.application.mapper.FormationMapper;
import com.enterprise.app.application.mapper.FormationParticipationMapper;
import com.enterprise.app.application.service.FormationService;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FormationController.class)
class FormationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FormationService formationService;

    @MockBean
    private FormationMapper formationMapper;

    @MockBean
    private FormationParticipationMapper participationMapper;

    private Formation formation;
    private FormationDTO formationDTO;
    private CreateFormationRequest createRequest;
    private Long formationId;
    private Secteur secteur;
    private Region region;

    @BeforeEach
    void setUp() {
        formationId = 1L;

        secteur = Secteur.builder()
                .id(1L)
                .code("IT")
                .nom("Informatique")
                .actif(true)
                .build();

        region = Region.builder()
                .id(1L)
                .code("IDF")
                .nom("Île-de-France")
                .actif(true)
                .build();

        formation = Formation.builder()
                .id(formationId)
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .description("Description test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur(secteur)
                .region(region)
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .lienParticipation("https://example.com")
                .build();

        formationDTO = FormationDTO.builder()
                .id(formationId)
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .description("Description test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur(SecteurDTO.builder().id(1L).code("IT").nom("Informatique").build())
                .region(RegionDTO.builder().id(1L).code("IDF").nom("Île-de-France").build())
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .lienParticipation("https://example.com")
                .statut(FormationStatut.A_VENIR)
                .nbParticipantsInscrits(5)
                .complet(false)
                .build();

        createRequest = CreateFormationRequest.builder()
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .description("Description test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteurId(1L)
                .regionId(1L)
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .build();

    }

    @Test
    @WithMockUser
    void getAllFormations_ShouldReturnFormations() throws Exception {
        // Given
        Page<Formation> dtoPage = new PageImpl<>(Collections.singletonList(FormationFixtures.defaultFormationPresentiel()));

        when(formationService.searchFormations(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(dtoPage);

        // When & Then
        mockMvc.perform(get("/api/v1/formations")
                .param("secteurId", "1")
                .param("regionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(formationId))
                .andExpect(jsonPath("$.content[0].libelle").value("Formation Test"));

        verify(formationService).searchFormations(eq(1L), eq(1L), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @WithMockUser
    void getFormationById_ShouldReturnFormation_WhenExists() throws Exception {
        // Given
        when(formationService.getFormationById(formationId)).thenReturn(formation);

        // When & Then
        mockMvc.perform(get("/api/v1/formations/{id}", formationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(formationId))
                .andExpect(jsonPath("$.libelle").value("Formation Test"));

        verify(formationService).getFormationById(formationId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createFormation_ShouldCreateFormation_WhenValidRequest() throws Exception {
        // Given
        when(formationMapper.toEntity(any(CreateFormationRequest.class))).thenReturn(formation);
        when(formationService.createFormation(any(Formation.class))).thenReturn(formation);
        when(formationMapper.toDTO(any(Formation.class))).thenReturn(formationDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/formations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(formationId))
                .andExpect(jsonPath("$.libelle").value("Formation Test"));

        verify(formationService).createFormation(any(Formation.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createFormation_ShouldReturnForbidden_WhenUserRole() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/formations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        verify(formationService, never()).createFormation(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateFormation_ShouldUpdateFormation_WhenValidRequest() throws Exception {
        // Given
        UpdateFormationRequest updateRequest = UpdateFormationRequest.builder()
                .libelle("Formation Updated")
                .formateurs("Formateur Updated")
                .description("Description updated")
                .dateFormation(LocalDate.now().plusDays(10))
                .heureDebut(LocalTime.of(10, 0))
                .heureFin(LocalTime.of(18, 0))
                .secteurId(1L)
                .regionId(1L)
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(25)
                .lieu("Lyon")
                .ville("Lyon")
                .build();

        Formation updatedFormation = formation.toBuilder()
                .libelle("Formation Updated")
                .build();

        when(formationService.getFormationById(formationId)).thenReturn(formation);
        when(formationService.updateFormation(eq(formationId), any(Formation.class))).thenReturn(updatedFormation);
        when(formationMapper.toDTO(updatedFormation)).thenReturn(formationDTO.toBuilder().libelle("Formation Updated").build());

        // When & Then
        mockMvc.perform(put("/api/v1/formations/{id}", formationId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(formationService).updateFormation(eq(formationId), any(Formation.class));
        verify(formationMapper).updateEntityFromDTO(eq(updateRequest), eq(formation));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteFormation_ShouldDeleteFormation() throws Exception {
        // Given
        doNothing().when(formationService).deleteFormation(formationId);

        // When & Then
        mockMvc.perform(delete("/api/v1/formations/{id}", formationId)
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(formationService).deleteFormation(formationId);
    }

    @Test
    @WithMockUser(roles = "TECH_LEAD")
    void getFormationParticipants_ShouldReturnParticipants() throws Exception {
        // Given
        FormationParticipation participation = FormationParticipation.builder()
                .formation(formation)
                .user(User.builder().id(2L).username("testuser").build())
                .statutParticipation(StatutParticipation.ABSENT)
                .build();

        List<FormationParticipation> participations = Arrays.asList(participation);
        when(formationService.getParticipantsFormation(formationId)).thenReturn(participations);

        // When & Then
        mockMvc.perform(get("/api/v1/formations/{id}/participants", formationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(formationService).getParticipantsFormation(formationId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getFormationParticipants_ShouldReturnForbidden_WhenUserRole() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/formations/{id}/participants", formationId))
                .andExpect(status().isForbidden());

        verify(formationService, never()).getParticipantsFormation(any());
    }

    @Test
    @WithMockUser
    void createFormation_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        // Given - Request with missing required fields
        CreateFormationRequest invalidRequest = CreateFormationRequest.builder()
                .libelle("") // Empty libelle
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/formations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(formationService, never()).createFormation(any());
    }
}