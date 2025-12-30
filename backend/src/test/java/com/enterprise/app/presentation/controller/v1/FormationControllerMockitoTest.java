package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.*;
import com.enterprise.app.application.mapper.FormationMapper;
import com.enterprise.app.application.mapper.FormationParticipationMapper;
import com.enterprise.app.application.service.FormationService;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.testing.fixtures.FormationParticipationFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static com.enterprise.app.testing.fixtures.FormationFixtures.*;
import static com.enterprise.app.testing.fixtures.FormationParticipationFixtures.defaultParticipationDTO;
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

    private Long formationId;
    private Formation formation;
    private FormationDTO formationDTO;
    private CreateFormationRequest createRequest;
    private UpdateFormationRequest updateRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        formationId = 1L;
        pageable = PageRequest.of(0, 20);

        formation = defaultFormationPresentiel();

        formationDTO = createDefaultFormationDTO();

        createRequest = createDefaultFormationRequest();

        updateRequest = createDefaultFormationUpdateRequest();
    }

    @Test
    @DisplayName("Doit retourner les formations paginées")
    void getAllFormations_ShouldReturnPaginatedFormations() {
        // Given
        Page<Formation> expectedPage = new PageImpl<>(Collections.singletonList(formation), pageable, 1);

        when(formationService.searchFormations(eq(1L), eq(1L),
                eq(ModaliteFormation.PRESENTIEL), eq(FormationStatut.A_VENIR), eq(pageable)))
                .thenReturn(expectedPage);
        when(formationMapper.toDTO(formation)).thenReturn(formationDTO);
        // When
        ResponseEntity<Page<FormationDTO>> response = formationController.getAllFormations(
                1L, 1L, ModaliteFormation.PRESENTIEL, FormationStatut.A_VENIR, pageable);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getLibelle()).isEqualTo("Tirage au sort");

        verify(formationService).searchFormations(1L, 1L,
                ModaliteFormation.PRESENTIEL, FormationStatut.A_VENIR, pageable);
    }

    @Test
    @DisplayName("Doit retourner toutes les formations avec des filtres nuls")
    void getAllFormations_WithNullFilters_ShouldReturnAllFormations() {
        // Given
        Page<Formation> projectionPage = new PageImpl<>(Collections.singletonList(formation), pageable, 1);

        when(formationService.searchFormations(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(projectionPage);
        when(formationMapper.toDTO(formation)).thenReturn(formationDTO);

        // When
        ResponseEntity<Page<FormationDTO>> response = formationController.getAllFormations(
                null, null, null, null, pageable);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(formationService).searchFormations(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("Doit retourner une formation quand elle existe")
    void getFormationById_ShouldReturnFormation_WhenExists() {
        // Given
        when(formationService.getFormationById(formationId)).thenReturn(formation);
        when(formationMapper.toDTO(formation)).thenReturn(formationDTO);
        // When
        ResponseEntity<FormationDTO> response = formationController.getFormationById(formationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(formationId);
        assertThat(response.getBody().getLibelle()).isEqualTo("Tirage au sort");

        verify(formationService).getFormationById(formationId);
    }

    @Test
    @DisplayName("Doit retourner la formation créée avec une requête valide")
    void createFormation_ShouldReturnCreatedFormation_WhenValidRequest() {
        // Given
        ArgumentCaptor<Formation> captor = ArgumentCaptor.forClass(Formation.class);

        when(formationMapper.toEntity(any()))
                .thenReturn(formation);

        when(formationService.createFormationWithIds(
                captor.capture(),
                anyLong(),
                anyLong()
        )).thenReturn(formation);

        when(formationMapper.toDTO(formation))
                .thenReturn(formationDTO);

        // When
        ResponseEntity<FormationDTO> response =
                formationController.createFormation(createRequest);

        // Then
        assertThat(captor.getValue()).isEqualTo(formation);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLibelle()).isEqualTo("Tirage au sort");

        verify(formationMapper).toEntity(any());
        verify(formationService).createFormationWithIds(any(), anyLong(), anyLong());
        verify(formationMapper).toDTO(formation);

    }

    @Test
    @DisplayName("Doit retourner la formation mise à jour avec une requête valide")
    void updateFormation_ShouldReturnUpdatedFormation_WhenValidRequest() {
        // Given
        Formation updatedFormation = formation.toBuilder()
                .libelle("Tirage au sort - Sécrétaire")
                .build();

        FormationDTO updatedDto = formationDTO.toBuilder()
                .libelle("Tirage au sort - Sécrétaire")
                .build();

        when(formationService.getFormationById(formationId)).thenReturn(formation);
        when(formationService.updateFormationWithIds(eq(formationId), eq(formation), anyLong(), anyLong())).thenReturn(updatedFormation);
        when(formationMapper.toDTO(updatedFormation)).thenReturn(updatedDto);

        // When
        ResponseEntity<FormationDTO> response = formationController.updateFormation(formationId, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLibelle()).isEqualTo("Tirage au sort - Sécrétaire");

        verify(formationService).getFormationById(formationId);
        verify(formationMapper).updateEntityFromDTO(updateRequest, formation);
        verify(formationService).updateFormationWithIds(eq(formationId), eq(formation), anyLong(), anyLong());
        verify(formationMapper).toDTO(updatedFormation);
    }

    @Test
    @DisplayName("Doit retourner aucun contenu quand la formation existe")
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
    @DisplayName("Doit retourner la liste des participants de la formation")
    void getFormationParticipants_ShouldReturnParticipantsList() {
        FormationParticipation participation = FormationParticipationFixtures.defaultParticipation();

        FormationParticipationDTO participationDTO = defaultParticipationDTO();

        List<FormationParticipation> participations = List.of(participation);
        when(formationService.getParticipantsFormation(formationId)).thenReturn(participations);
        when(participationMapper.toDTO(participation)).thenReturn(participationDTO);

        // When
        ResponseEntity<List<FormationParticipationDTO>> response =
                formationController.getFormationParticipants(formationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getStatutParticipation()).isEqualTo(StatutParticipation.ABSENT);

        verify(formationService).getParticipantsFormation(formationId);
        verify(participationMapper).toDTO(participation);
    }

    @Test
    @DisplayName("Doit retourner une liste vide quand il n'y a pas de participants")
    void getFormationParticipants_ShouldReturnEmptyList_WhenNoParticipants() {
        // Given
        when(formationService.getParticipantsFormation(formationId)).thenReturn(List.of());

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
}