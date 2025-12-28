package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.FormationParticipationDTO;
import com.enterprise.app.application.dto.PresenceRequest;
import com.enterprise.app.application.mapper.FormationParticipationMapper;
import com.enterprise.app.application.service.FormationService;
import com.enterprise.app.application.usecase.UserService;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import com.enterprise.app.testing.fixtures.FormationParticipationFixtures;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;


import static com.enterprise.app.testing.fixtures.FormationParticipationFixtures.defaultParticipationDTO;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FormationParticipationController.class)
class FormationParticipationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FormationService formationService;

    @MockBean
    private FormationParticipationMapper participationMapper;

    @MockBean
    private UserService userService;

    private Formation formation;
    private User user;
    private FormationParticipation participation;
    private FormationParticipationDTO participationDTO;
    private Long formationId;
    private Long userId;

    @BeforeEach
    void setUp() {
        formationId = 1L;
        userId = 1L;

        formation = FormationFixtures.defaultFormationPresentiel();

        user = User.builder()
            .id(userId)
            .username("testuser")
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .active(true)
            .build();

        participation = FormationParticipationFixtures.defaultParticipation();

        participationDTO = defaultParticipationDTO();
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void inscrireUtilisateur_ShouldCreateInscription_WhenValidRequest() throws Exception {
        // Given
        when(userService.getUserEntityByUsername("testuser")).thenReturn(user);
        when(formationService.inscrireUtilisateur(formationId, userId)).thenReturn(participation);
        when(participationMapper.toDTO(participation)).thenReturn(participationDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/formations/{formationId}/inscriptions", formationId)
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.formationId").value(formationId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.statutParticipation").value("ABSENT"));

        verify(userService).getUserEntityByUsername("testuser");
        verify(formationService).inscrireUtilisateur(formationId, userId);
        verify(participationMapper).toDTO(participation);
    }



    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void desinscrireUtilisateur_ShouldRemoveInscription_WhenValidRequest() throws Exception {
        // Given
        when(userService.getUserEntityByUsername("testuser")).thenReturn(user);
        doNothing().when(formationService).desinscrireUtilisateur(formationId, userId);

        // When & Then
        mockMvc.perform(delete("/api/v1/formations/{formationId}/inscriptions", formationId)
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).getUserEntityByUsername("testuser");
        verify(formationService).desinscrireUtilisateur(formationId, userId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void marquerPresence_ShouldUpdatePresence_WhenValidRequest() throws Exception {
        // Given
        PresenceRequest presenceRequest = new PresenceRequest();
        presenceRequest.setPresent(true);
        presenceRequest.setCommentaire("Présent toute la journée");

        FormationParticipation updatedParticipation = participation.toBuilder()
                .statutParticipation(StatutParticipation.PRESENT)
                .datePresence(LocalDateTime.now())
                .commentaire("Présent toute la journée")
                .build();

        FormationParticipationDTO updatedDTO = participationDTO.toBuilder()
                .statutParticipation(StatutParticipation.PRESENT)
                .datePresence(LocalDateTime.now())
                .commentaire("Présent toute la journée")
                .build();

        when(formationService.marquerPresence(formationId, userId, true)).thenReturn(updatedParticipation);
        when(participationMapper.toDTO(updatedParticipation)).thenReturn(updatedDTO);

        // When & Then
        mockMvc.perform(put("/api/v1/formations/{formationId}/participants/{userId}/presence", formationId, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presenceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutParticipation").value("PRESENT"))
                .andExpect(jsonPath("$.commentaire").value("Présent toute la journée"));

        verify(formationService).marquerPresence(formationId, userId, true);
        verify(participationMapper).toDTO(updatedParticipation);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void marquerPresence_ShouldMarkAbsent_WhenPresenceFalse() throws Exception {
        // Given
        PresenceRequest presenceRequest = new PresenceRequest();
        presenceRequest.setPresent(false);
        presenceRequest.setCommentaire("Absent pour raisons personnelles");

        FormationParticipation updatedParticipation = participation.toBuilder()
                .statutParticipation(StatutParticipation.ABSENT)
                .datePresence(null)
                .commentaire("Absent pour raisons personnelles")
                .build();

        FormationParticipationDTO updatedDTO = participationDTO.toBuilder()
                .statutParticipation(StatutParticipation.ABSENT)
                .datePresence(null)
                .commentaire("Absent pour raisons personnelles")
                .build();

        when(formationService.marquerPresence(formationId, userId, false)).thenReturn(updatedParticipation);
        when(participationMapper.toDTO(updatedParticipation)).thenReturn(updatedDTO);

        // When & Then
        mockMvc.perform(put("/api/v1/formations/{formationId}/participants/{userId}/presence", formationId, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presenceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutParticipation").value("ABSENT"));

        verify(formationService).marquerPresence(formationId, userId, false);
    }

    @Test
    @WithMockUser(roles = "USER")
    void marquerPresence_ShouldReturnForbidden_WhenUserRole() throws Exception {
        // Given
        PresenceRequest presenceRequest = new PresenceRequest();
        presenceRequest.setPresent(true);

        // When & Then
        mockMvc.perform(put("/api/v1/formations/{formationId}/participants/{userId}/presence", formationId, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(presenceRequest)))
                .andExpect(status().isForbidden());

        verify(formationService, never()).marquerPresence(any(), any(), anyBoolean());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void marquerPresence_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Given - Request without required field
        String invalidJson = "{}";

        // When & Then
        mockMvc.perform(put("/api/v1/formations/{formationId}/participants/{userId}/presence", formationId, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(formationService, never()).marquerPresence(any(), any(), anyBoolean());
    }

    @Test
    void inscrireUtilisateur_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/formations/{formationId}/inscriptions/{userId}", formationId, userId)
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(formationService, never()).inscrireUtilisateur(any(), any());
    }

}