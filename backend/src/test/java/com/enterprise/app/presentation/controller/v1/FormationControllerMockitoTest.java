package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.CreateFormationRequest;
import com.enterprise.app.application.dto.FormationDTO;
import com.enterprise.app.application.dto.FormationParticipationDTO;
import com.enterprise.app.application.dto.UpdateFormationRequest;
import com.enterprise.app.application.mapper.FormationMapper;
import com.enterprise.app.application.mapper.FormationParticipationMapper;
import com.enterprise.app.application.service.FormationService;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.infrastructure.persistence.projection.FormationProjection;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormationControllerMockitoTest {

    @Mock
    private FormationService formationService;

    @Mock
    private FormationMapper formationMapper;

    @Mock
    private FormationParticipationMapper participationMapper;

    @InjectMocks
    private FormationController formationController;

    private UUID formationId;
    private Formation formation;
    private FormationDTO formationDTO;
    private FormationProjection formationProjection;
    private CreateFormationRequest createRequest;
    private UpdateFormationRequest updateRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        formationId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);

        formation = Formation.builder()
                .id(formationId)
                .libelle("Formation Java Avancé")
                .formateurs("Jean Dupont")
                .description("Formation approfondie sur Java")
                .dateFormation(LocalDate.now().plusDays(15))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("Informatique")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Salle A")
                .ville("Paris")
                .lienParticipation("https://formation.example.com")
                .build();

        formationDTO = FormationDTO.builder()
                .id(formationId)
                .libelle("Formation Java Avancé")
                .formateurs("Jean Dupont")
                .description("Formation approfondie sur Java")
                .dateFormation(LocalDate.now().plusDays(15))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("Informatique")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Salle A")
                .ville("Paris")
                .lienParticipation("https://formation.example.com")
                .statut(FormationStatut.A_VENIR)
                .nbParticipantsInscrits(8)
                .complet(false)
                .build();

        formationProjection = createMockProjection();

        createRequest = CreateFormationRequest.builder()
                .libelle("Formation Java Avancé")
                .formateurs("Jean Dupont")
                .description("Formation approfondie sur Java")
                .dateFormation(LocalDate.now().plusDays(15))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("Informatique")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Salle A")
                .ville("Paris")
                .build();

        updateRequest = UpdateFormationRequest.builder()
                .libelle("Formation Java Expert")
                .formateurs("Marie Martin")
                .description("Formation expert sur Java")
                .dateFormation(LocalDate.now().plusDays(20))
                .heureDebut(LocalTime.of(10, 0))
                .heureFin(LocalTime.of(18, 0))
                .secteur("Informatique")
                .region("Provence-Alpes-Côte d'Azur")
                .modalite(ModaliteFormation.HYBRIDE)
                .nbParticipants(15)
                .lieu("Salle B")
                .ville("Marseille")
                .build();
    }

    @Test
    void getAllFormations_ShouldReturnPaginatedFormations() {
        // Given
        Page<FormationProjection> projectionPage = new PageImpl<>(Arrays.asList(formationProjection), pageable, 1);
        Page<FormationDTO> expectedDtoPage = new PageImpl<>(Arrays.asList(formationDTO), pageable, 1);

        when(formationService.searchFormationProjections(eq("Informatique"), eq("Île-de-France"),
                eq(ModaliteFormation.PRESENTIEL), eq(FormationStatut.A_VENIR), eq(pageable)))
                .thenReturn(projectionPage);
        when(formationMapper.toDTO(formationProjection)).thenReturn(formationDTO);

        // When
        ResponseEntity<Page<FormationDTO>> response = formationController.getAllFormations(
                "Informatique", "Île-de-France", ModaliteFormation.PRESENTIEL, FormationStatut.A_VENIR, pageable);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getLibelle()).isEqualTo("Formation Java Avancé");

        verify(formationService).searchFormationProjections("Informatique", "Île-de-France",
                ModaliteFormation.PRESENTIEL, FormationStatut.A_VENIR, pageable);
        verify(formationMapper).toDTO(formationProjection);
    }

    @Test
    void getAllFormations_WithNullFilters_ShouldReturnAllFormations() {
        // Given
        Page<FormationProjection> projectionPage = new PageImpl<>(Arrays.asList(formationProjection), pageable, 1);

        when(formationService.searchFormationProjections(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(projectionPage);
        when(formationMapper.toDTO(formationProjection)).thenReturn(formationDTO);

        // When
        ResponseEntity<Page<FormationDTO>> response = formationController.getAllFormations(
                null, null, null, null, pageable);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(formationService).searchFormationProjections(null, null, null, null, pageable);
    }

    @Test
    void getFormationById_ShouldReturnFormation_WhenExists() {
        // Given
        when(formationService.getFormationProjectionById(formationId)).thenReturn(formationProjection);
        when(formationMapper.toDTO(formationProjection)).thenReturn(formationDTO);

        // When
        ResponseEntity<FormationDTO> response = formationController.getFormationById(formationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(formationId);
        assertThat(response.getBody().getLibelle()).isEqualTo("Formation Java Avancé");

        verify(formationService).getFormationProjectionById(formationId);
        verify(formationMapper).toDTO(formationProjection);
    }

    @Test
    void createFormation_ShouldReturnCreatedFormation_WhenValidRequest() {
        // Given
        when(formationMapper.toEntity(createRequest)).thenReturn(formation);
        when(formationService.createFormation(formation)).thenReturn(formation);
        when(formationMapper.toDTO(formation)).thenReturn(formationDTO);

        // When
        ResponseEntity<FormationDTO> response = formationController.createFormation(createRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLibelle()).isEqualTo("Formation Java Avancé");

        verify(formationMapper).toEntity(createRequest);
        verify(formationService).createFormation(formation);
        verify(formationMapper).toDTO(formation);
    }

    @Test
    void updateFormation_ShouldReturnUpdatedFormation_WhenValidRequest() {
        // Given
        Formation updatedFormation = formation.toBuilder()
                .libelle("Formation Java Expert")
                .build();

        FormationDTO updatedDto = formationDTO.toBuilder()
                .libelle("Formation Java Expert")
                .build();

        when(formationService.getFormationById(formationId)).thenReturn(formation);
        when(formationService.updateFormation(formationId, formation)).thenReturn(updatedFormation);
        when(formationMapper.toDTO(updatedFormation)).thenReturn(updatedDto);

        // When
        ResponseEntity<FormationDTO> response = formationController.updateFormation(formationId, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLibelle()).isEqualTo("Formation Java Expert");

        verify(formationService).getFormationById(formationId);
        verify(formationMapper).updateEntityFromDTO(updateRequest, formation);
        verify(formationService).updateFormation(formationId, formation);
        verify(formationMapper).toDTO(updatedFormation);
    }

    @Test
    void deleteFormation_ShouldReturnNoContent_WhenFormationExists() {
        // Given
        doNothing().when(formationService).deleteFormation(formationId);

        // When
        ResponseEntity<Void> response = formationController.deleteFormation(formationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(formationService).deleteFormation(formationId);
    }

    @Test
    void getFormationParticipants_ShouldReturnParticipantsList() {
        // Given
        User participant = User.builder()
                .id(UUID.randomUUID())
                .username("participant.test")
                .nom("Test")
                .prenom("Participant")
                .build();

        FormationParticipation participation = FormationParticipation.builder()
                .id(UUID.randomUUID())
                .formation(formation)
                .user(participant)
                .statutParticipation(StatutParticipation.INSCRIT)
                .build();

        FormationParticipationDTO participationDTO = FormationParticipationDTO.builder()
                .id(participation.getId())
                .formationId(formationId)
                .userId(participant.getId())
                .statutParticipation(StatutParticipation.INSCRIT)
                .build();

        List<FormationParticipation> participations = Arrays.asList(participation);
        when(formationService.getParticipantsFormation(formationId)).thenReturn(participations);
        when(participationMapper.toDTO(participation)).thenReturn(participationDTO);

        // When
        ResponseEntity<List<FormationParticipationDTO>> response =
                formationController.getFormationParticipants(formationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getStatutParticipation()).isEqualTo(StatutParticipation.INSCRIT);

        verify(formationService).getParticipantsFormation(formationId);
        verify(participationMapper).toDTO(participation);
    }

    @Test
    void getFormationParticipants_ShouldReturnEmptyList_WhenNoParticipants() {
        // Given
        when(formationService.getParticipantsFormation(formationId)).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<FormationParticipationDTO>> response =
                formationController.getFormationParticipants(formationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();

        verify(formationService).getParticipantsFormation(formationId);
        verify(participationMapper, never()).toDTO(any());
    }

    private FormationProjection createMockProjection() {
        return new FormationProjection() {
            @Override
            public UUID getId() { return formationId; }
            @Override
            public String getLibelle() { return "Formation Java Avancé"; }
            @Override
            public String getFormateurs() { return "Jean Dupont"; }
            @Override
            public String getDescription() { return "Formation approfondie sur Java"; }
            @Override
            public LocalDate getDateFormation() { return LocalDate.now().plusDays(15); }
            @Override
            public LocalTime getHeureDebut() { return LocalTime.of(9, 0); }
            @Override
            public LocalTime getHeureFin() { return LocalTime.of(17, 0); }
            @Override
            public String getSecteur() { return "Informatique"; }
            @Override
            public String getRegion() { return "Île-de-France"; }
            @Override
            public ModaliteFormation getModalite() { return ModaliteFormation.PRESENTIEL; }
            @Override
            public Integer getNbParticipants() { return 20; }
            @Override
            public String getLieu() { return "Salle A"; }
            @Override
            public String getVille() { return "Paris"; }
            @Override
            public String getLienParticipation() { return "https://formation.example.com"; }
            @Override
            public FormationStatut getStatut() { return FormationStatut.A_VENIR; }
            @Override
            public LocalDateTime getCreatedAt() { return LocalDateTime.now(); }
            @Override
            public LocalDateTime getUpdatedAt() { return LocalDateTime.now(); }
            @Override
            public Integer getNbParticipantsInscrits() { return 8; }
            @Override
            public boolean isComplet() { return false; }
        };
    }
}