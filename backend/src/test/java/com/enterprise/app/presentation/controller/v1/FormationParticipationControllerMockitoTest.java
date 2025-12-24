package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.FormationParticipationDTO;
import com.enterprise.app.application.dto.PresenceRequest;
import com.enterprise.app.application.mapper.FormationParticipationMapper;
import com.enterprise.app.application.service.FormationService;
import com.enterprise.app.application.usecase.UserService;
import com.enterprise.app.domain.model.*;
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
import java.util.UUID;

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

    private UUID formationId;
    private UUID userId;
    private UUID participationId;
    private Formation formation;
    private User user;
    private FormationParticipation participation;
    private FormationParticipationDTO participationDTO;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        formationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        participationId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);

        formation = Formation.builder()
                .id(formationId)
                .libelle("Formation Spring Boot")
                .formateurs("Sophie Dubois")
                .description("Formation complète sur Spring Boot")
                .dateFormation(LocalDate.now().plusDays(10))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("Développement")
                .region("Auvergne-Rhône-Alpes")
                .modalite(ModaliteFormation.HYBRIDE)
                .nbParticipants(25)
                .lieu("Centre de formation")
                .ville("Lyon")
                .lienParticipation("https://meet.spring-boot.com")
                .build();

        user = User.builder()
                .id(userId)
                .username("jean.martin")
                .email("jean.martin@example.com")
                .firstName("Jean")
                .lastName("Martin")
                .nom("Martin")
                .prenom("Jean")
                .active(true)
                .build();

        participation = FormationParticipation.builder()
                .id(participationId)
                .formation(formation)
                .user(user)
                .statutParticipation(StatutParticipation.INSCRIT)
                .dateInscription(LocalDateTime.now().minusDays(2))
                .build();

        participationDTO = FormationParticipationDTO.builder()
                .id(participationId)
                .formationId(formationId)
                .userId(userId)
                .statutParticipation(StatutParticipation.INSCRIT)
                .dateInscription(LocalDateTime.now().minusDays(2))
                .build();
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
        assertThat(response.getBody().getStatutParticipation()).isEqualTo(StatutParticipation.INSCRIT);

        verify(userService).getUserEntityByUsername("jean.martin");
        verify(formationService).inscrireUtilisateur(formationId, userId);
        verify(participationMapper).toDTO(participation);
    }

    @Test
    void inscrireUtilisateur_ShouldUseSubjectWhenPreferredUsernameIsNull() {
        // Given
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(null);
        when(jwt.getSubject()).thenReturn("jean.martin");
        when(userService.getUserEntityByUsername("jean.martin")).thenReturn(user);
        when(formationService.inscrireUtilisateur(formationId, userId)).thenReturn(participation);
        when(participationMapper.toDTO(participation)).thenReturn(participationDTO);

        // When
        ResponseEntity<FormationParticipationDTO> response =
                formationParticipationController.inscrireUtilisateur(formationId, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userService).getUserEntityByUsername("jean.martin");
        verify(jwt).getSubject();
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
    void getFormationsUtilisateur_ShouldReturnUserFormations() {
        // Given
        setupMockAuthentication();
        Page<FormationParticipation> participationPage = new PageImpl<>(Arrays.asList(participation), pageable, 1);
        Page<FormationParticipationDTO> expectedDtoPage = new PageImpl<>(Arrays.asList(participationDTO), pageable, 1);

        when(userService.getUserEntityByUsername("jean.martin")).thenReturn(user);
        when(formationService.getFormationsUtilisateur(userId, pageable)).thenReturn(participationPage);
        when(participationMapper.toDTO(participation)).thenReturn(participationDTO);

        // When
        ResponseEntity<Page<FormationParticipationDTO>> response =
                formationParticipationController.getFormationsUtilisateur(pageable, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getId()).isEqualTo(participationId);
        assertThat(response.getBody().getContent().get(0).getStatutParticipation()).isEqualTo(StatutParticipation.INSCRIT);

        verify(userService).getUserEntityByUsername("jean.martin");
        verify(formationService).getFormationsUtilisateur(userId, pageable);
        verify(participationMapper).toDTO(participation);
    }

    @Test
    void getFormationsUtilisateur_ShouldReturnEmptyPage_WhenNoFormations() {
        // Given
        setupMockAuthentication();
        Page<FormationParticipation> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        when(userService.getUserEntityByUsername("jean.martin")).thenReturn(user);
        when(formationService.getFormationsUtilisateur(userId, pageable)).thenReturn(emptyPage);

        // When
        ResponseEntity<Page<FormationParticipationDTO>> response =
                formationParticipationController.getFormationsUtilisateur(pageable, authentication);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);

        verify(userService).getUserEntityByUsername("jean.martin");
        verify(formationService).getFormationsUtilisateur(userId, pageable);
        verify(participationMapper, never()).toDTO(any());
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

    @Test
    void marquerPresence_ShouldNotSetCommentaire_WhenCommentaireIsNull() {
        // Given
        PresenceRequest presenceRequest = new PresenceRequest();
        presenceRequest.setPresent(true);
        presenceRequest.setCommentaire(null);

        FormationParticipation updatedParticipation = participation.toBuilder()
                .statutParticipation(StatutParticipation.PRESENT)
                .datePresence(LocalDateTime.now())
                .build();

        FormationParticipationDTO updatedDto = participationDTO.toBuilder()
                .statutParticipation(StatutParticipation.PRESENT)
                .datePresence(LocalDateTime.now())
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

        verify(formationService).marquerPresence(formationId, userId, true);
        verify(participationMapper).toDTO(updatedParticipation);
        verify(updatedParticipation, never()).setCommentaire(anyString());
    }

    @Test
    void marquerPresence_ShouldSetCommentaire_WhenCommentaireProvided() {
        // Given
        PresenceRequest presenceRequest = new PresenceRequest();
        presenceRequest.setPresent(false);
        presenceRequest.setCommentaire("Formation annulée");

        FormationParticipation updatedParticipation = spy(participation.toBuilder()
                .statutParticipation(StatutParticipation.ABSENT)
                .build());

        FormationParticipationDTO updatedDto = participationDTO.toBuilder()
                .statutParticipation(StatutParticipation.ABSENT)
                .commentaire("Formation annulée")
                .build();

        when(formationService.marquerPresence(formationId, userId, false)).thenReturn(updatedParticipation);
        when(participationMapper.toDTO(updatedParticipation)).thenReturn(updatedDto);

        // When
        ResponseEntity<FormationParticipationDTO> response =
                formationParticipationController.marquerPresence(formationId, userId, presenceRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(formationService).marquerPresence(formationId, userId, false);
        verify(updatedParticipation).setCommentaire("Formation annulée");
        verify(participationMapper).toDTO(updatedParticipation);
    }

    private void setupMockAuthentication() {
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("jean.martin");
    }
}