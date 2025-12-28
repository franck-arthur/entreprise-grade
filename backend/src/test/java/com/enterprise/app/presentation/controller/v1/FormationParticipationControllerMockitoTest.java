package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.FormationParticipationDTO;
import com.enterprise.app.application.dto.PresenceRequest;
import com.enterprise.app.application.mapper.FormationParticipationMapper;
import com.enterprise.app.application.service.FormationService;
import com.enterprise.app.application.usecase.UserService;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import com.enterprise.app.testing.fixtures.FormationParticipationFixtures;
import com.enterprise.app.testing.fixtures.SecteurFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.enterprise.app.testing.fixtures.FormationParticipationFixtures.defaultParticipationDTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormationParticipationControllerMockitoTest {

    @Mock
    private FormationService formationService;

    @Mock
    private FormationParticipationMapper participationMapper;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private FormationParticipationController formationParticipationController;

    private Long formationId;
    private Long userId;
    private User user;
    private FormationParticipation participation;
    private FormationParticipationDTO participationDTO;

    @BeforeEach
    void setUp() {
        formationId = 1L;
        userId = 1L;

        user = User.builder()
                .id(userId)
                .username("jean.martin")
                .email("jean.martin@example.com")
                .firstName("Jean")
                .lastName("Martin")
                .active(true)
                .build();

        participation = FormationParticipationFixtures.defaultParticipation();

        participationDTO = defaultParticipationDTO();
    }

    @Test
    void inscrireUtilisateur_ShouldCreateInscription_WhenValidRequest() {
        // Given
        setupMockAuthentication();
        when(userService.getUserEntityByUsername("jean.martin")).thenReturn(user);
        when(formationService.inscrireUtilisateur(formationId, userId)).thenReturn(participation);
        when(participationMapper.toDTO(participation)).thenReturn(participationDTO);

        // When
        ResponseEntity<FormationParticipationDTO> response =
                formationParticipationController.inscrireUtilisateur(formationId, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFormationId()).isEqualTo(formationId);
        assertThat(response.getBody().getUserId()).isEqualTo(userId);
        assertThat(response.getBody().getStatutParticipation()).isEqualTo(StatutParticipation.ABSENT);

        verify(userService).getUserEntityByUsername("jean.martin");
        verify(formationService).inscrireUtilisateur(formationId, userId);
        verify(participationMapper).toDTO(participation);
    }

    @Test
    void desinscrireUtilisateur_ShouldRemoveInscription_WhenValidRequest() {
        // Given
        setupMockAuthentication();
        when(userService.getUserEntityByUsername("jean.martin")).thenReturn(user);
        doNothing().when(formationService).desinscrireUtilisateur(formationId, userId);

        // When
        ResponseEntity<Void> response =
                formationParticipationController.desinscrireUtilisateur(formationId, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(userService).getUserEntityByUsername("jean.martin");
        verify(formationService).desinscrireUtilisateur(formationId, userId);
    }

    @Test
    void marquerPresence_ShouldMarkPresent_WhenValidRequest() {
        // Given
        PresenceRequest presenceRequest = new PresenceRequest();
        presenceRequest.setPresent(true);
        presenceRequest.setCommentaire("Participé activement");

        FormationParticipation updatedParticipation = participation.toBuilder()
                .statutParticipation(StatutParticipation.PRESENT)
                .datePresence(LocalDateTime.now())
                .commentaire("Participé activement")
                .build();

        FormationParticipationDTO updatedDto = participationDTO.toBuilder()
                .statutParticipation(StatutParticipation.PRESENT)
                .datePresence(LocalDateTime.now())
                .commentaire("Participé activement")
                .build();

        when(formationService.marquerPresence(formationId, userId, true)).thenReturn(updatedParticipation);
        when(participationMapper.toDTO(updatedParticipation)).thenReturn(updatedDto);

        // When
        ResponseEntity<FormationParticipationDTO> response =
                formationParticipationController.marquerPresence(formationId, userId, presenceRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatutParticipation()).isEqualTo(StatutParticipation.PRESENT);
        assertThat(response.getBody().getCommentaire()).isEqualTo("Participé activement");

        verify(formationService).marquerPresence(formationId, userId, true);
        verify(participationMapper).toDTO(updatedParticipation);
    }

    @Test
    void marquerPresence_ShouldMarkAbsent_WhenPresenceFalse() {
        // Given
        PresenceRequest presenceRequest = new PresenceRequest();
        presenceRequest.setPresent(false);
        presenceRequest.setCommentaire("Absent justifié");

        FormationParticipation updatedParticipation = participation.toBuilder()
                .statutParticipation(StatutParticipation.ABSENT)
                .datePresence(null)
                .commentaire("Absent justifié")
                .build();

        FormationParticipationDTO updatedDto = participationDTO.toBuilder()
                .statutParticipation(StatutParticipation.ABSENT)
                .datePresence(null)
                .commentaire("Absent justifié")
                .build();

        when(formationService.marquerPresence(formationId, userId, false)).thenReturn(updatedParticipation);
        when(participationMapper.toDTO(updatedParticipation)).thenReturn(updatedDto);

        // When
        ResponseEntity<FormationParticipationDTO> response =
                formationParticipationController.marquerPresence(formationId, userId, presenceRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatutParticipation()).isEqualTo(StatutParticipation.ABSENT);
        assertThat(response.getBody().getCommentaire()).isEqualTo("Absent justifié");

        verify(formationService).marquerPresence(formationId, userId, false);
        verify(participationMapper).toDTO(updatedParticipation);
    }

    private void setupMockAuthentication() {
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("jean.martin");
    }
}