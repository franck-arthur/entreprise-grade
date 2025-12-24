package com.enterprise.app.testing.mocks;

import com.enterprise.app.application.dto.CreateFormationRequest;
import com.enterprise.app.application.dto.FormationDTO;
import com.enterprise.app.application.dto.UpdateFormationRequest;
import com.enterprise.app.application.mapper.FormationMapper;
import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.FormationStatut;
import com.enterprise.app.infrastructure.persistence.projection.FormationProjection;
import com.enterprise.app.testing.fixtures.FormationFixtures;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Provider de mocks configurés pour FormationMapper.
 */
public class FormationMapperMockProvider {

    /**
     * Crée un mock avec des comportements de mapping standard.
     */
    public static FormationMapper createStandardMock() {
        FormationMapper mock = mock(FormationMapper.class);

        // Mapping Entity vers DTO
        when(mock.toDTO(any(Formation.class)))
                .thenAnswer(invocation -> {
                    Formation formation = invocation.getArgument(0);
                    return createDTOFromFormation(formation);
                });

        // Mapping Projection vers DTO
        when(mock.toDTO(any(FormationProjection.class)))
                .thenAnswer(invocation -> {
                    FormationProjection projection = invocation.getArgument(0);
                    return createDTOFromProjection(projection);
                });

        // Mapping CreateRequest vers Entity
        when(mock.toEntity(any(CreateFormationRequest.class)))
                .thenAnswer(invocation -> {
                    CreateFormationRequest request = invocation.getArgument(0);
                    return createFormationFromCreateRequest(request);
                });

        // Update entity from DTO
        doNothing().when(mock).updateEntityFromDTO(any(UpdateFormationRequest.class), any(Formation.class));

        return mock;
    }

    /**
     * Crée un mock qui retourne toujours les mêmes DTOs par défaut.
     */
    public static FormationMapper createDefaultMock() {
        FormationMapper mock = mock(FormationMapper.class);

        FormationDTO defaultDTO = createDefaultFormationDTO();

        when(mock.toDTO(any(Formation.class))).thenReturn(defaultDTO);
        when(mock.toDTO(any(FormationProjection.class))).thenReturn(defaultDTO);
        when(mock.toEntity(any(CreateFormationRequest.class)))
                .thenReturn(FormationFixtures.defaultFormationPresentiel());

        return mock;
    }

    /**
     * Configure un mock existant avec un DTO spécifique.
     */
    public static void configureWithDTO(FormationMapper mock, FormationDTO dto) {
        when(mock.toDTO(any(Formation.class))).thenReturn(dto);
        when(mock.toDTO(any(FormationProjection.class))).thenReturn(dto);
    }

    /**
     * Configure un mock existant avec une formation spécifique.
     */
    public static void configureWithFormation(FormationMapper mock, Formation formation) {
        when(mock.toEntity(any(CreateFormationRequest.class))).thenReturn(formation);
    }

    /**
     * Crée un FormationDTO par défaut.
     */
    private static FormationDTO createDefaultFormationDTO() {
        Formation formation = FormationFixtures.defaultFormationPresentiel();
        return FormationDTO.builder()
                .id(formation.getId())
                .libelle(formation.getLibelle())
                .formateurs(formation.getFormateurs())
                .description(formation.getDescription())
                .dateFormation(formation.getDateFormation())
                .heureDebut(formation.getHeureDebut())
                .heureFin(formation.getHeureFin())
                .secteur(formation.getSecteur())
                .region(formation.getRegion())
                .modalite(formation.getModalite())
                .nbParticipants(formation.getNbParticipants())
                .lieu(formation.getLieu())
                .ville(formation.getVille())
                .lienParticipation(formation.getLienParticipation())
                .statut(FormationStatut.A_VENIR)
                .nbParticipantsInscrits(5)
                .complet(false)
                .build();
    }

    /**
     * Crée un FormationDTO à partir d'une Formation.
     */
    private static FormationDTO createDTOFromFormation(Formation formation) {
        return FormationDTO.builder()
                .id(formation.getId())
                .libelle(formation.getLibelle())
                .formateurs(formation.getFormateurs())
                .description(formation.getDescription())
                .dateFormation(formation.getDateFormation())
                .heureDebut(formation.getHeureDebut())
                .heureFin(formation.getHeureFin())
                .secteur(formation.getSecteur())
                .region(formation.getRegion())
                .modalite(formation.getModalite())
                .nbParticipants(formation.getNbParticipants())
                .lieu(formation.getLieu())
                .ville(formation.getVille())
                .lienParticipation(formation.getLienParticipation())
                .statut(formation.getStatut())
                .nbParticipantsInscrits(0)
                .complet(false)
                .build();
    }

    /**
     * Crée un FormationDTO à partir d'une FormationProjection.
     */
    private static FormationDTO createDTOFromProjection(FormationProjection projection) {
        return FormationDTO.builder()
                .id(projection.getId())
                .libelle(projection.getLibelle())
                .formateurs(projection.getFormateurs())
                .description(projection.getDescription())
                .dateFormation(projection.getDateFormation())
                .heureDebut(projection.getHeureDebut())
                .heureFin(projection.getHeureFin())
                .secteur(projection.getSecteur())
                .region(projection.getRegion())
                .modalite(projection.getModalite())
                .nbParticipants(projection.getNbParticipants())
                .lieu(projection.getLieu())
                .ville(projection.getVille())
                .lienParticipation(projection.getLienParticipation())
                .statut(projection.getStatut())
                .nbParticipantsInscrits(projection.getNbParticipantsInscrits())
                .complet(projection.isComplet())
                .build();
    }

    /**
     * Crée une Formation à partir d'un CreateFormationRequest.
     */
    private static Formation createFormationFromCreateRequest(CreateFormationRequest request) {
        return Formation.builder()
                .libelle(request.getLibelle())
                .formateurs(request.getFormateurs())
                .description(request.getDescription())
                .dateFormation(request.getDateFormation())
                .heureDebut(request.getHeureDebut())
                .heureFin(request.getHeureFin())
                .secteur(request.getSecteur())
                .region(request.getRegion())
                .modalite(request.getModalite())
                .nbParticipants(request.getNbParticipants())
                .lieu(request.getLieu())
                .ville(request.getVille())
                .lienParticipation(request.getLienParticipation())
                .build();
    }

    /**
     * Vérifie les interactions standard de mapping.
     */
    public static void verifyStandardMappingInteractions(FormationMapper mock) {
        verify(mock).toDTO(any(Formation.class));
        verifyNoMoreInteractions(mock);
    }
}