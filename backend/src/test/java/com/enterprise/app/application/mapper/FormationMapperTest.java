package com.enterprise.app.application.mapper;

import com.enterprise.app.application.dto.CreateFormationRequest;
import com.enterprise.app.application.dto.FormationDTO;
import com.enterprise.app.application.dto.UpdateFormationRequest;
import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.domain.repository.FormationParticipationRepository;
import com.enterprise.app.testing.fixtures.FormationFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static com.enterprise.app.domain.model.FormationStatut.A_VENIR;
import static com.enterprise.app.testing.fixtures.FormationFixtures.createDefaultFormationUpdateRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormationMapperTest {

    @Mock
    private FormationParticipationRepository participationRepository;

    @Mock
    private SecteurMapper secteurMapper;

    @Mock
    private RegionMapper regionMapper;

    @InjectMocks
    private FormationMapperImpl mapper;

    @BeforeEach
    void setUp() {
        // Configuration des mocks par défaut
        lenient().when(participationRepository.countByFormationId(1L)).thenReturn(5);
        lenient().when(participationRepository.countByFormationId(2L)).thenReturn(30);
        lenient().when(participationRepository.countByFormationId(3L)).thenReturn(0);

        // Configuration des mappers
        lenient().when(secteurMapper.toDTO(any())).thenReturn(null);
        lenient().when(regionMapper.toDTO(any())).thenReturn(null);
    }

    @Test
    void shouldMapCreateRequestToEntity() {
        CreateFormationRequest request = FormationFixtures.createDefaultFormationRequest();

        Formation formation = mapper.toEntity(request);

        assertThat(formation).isNotNull();
        assertThat(formation.getLibelle()).isEqualTo(request.getLibelle());
        assertThat(formation.getFormateurs()).isEqualTo(request.getFormateurs());
        assertThat(formation.getModalite()).isEqualTo(ModaliteFormation.PRESENTIEL);
        assertThat(formation.getNbParticipants()).isEqualTo(30);
        assertThat(formation.getDateFormation()).isEqualTo(request.getDateFormation());
        assertThat(formation.getHeureDebut()).isEqualTo(request.getHeureDebut());
        assertThat(formation.getHeureFin()).isEqualTo(request.getHeureFin());
        assertThat(formation.getDescription()).isEqualTo(request.getDescription());
        assertThat(formation.getLieu()).isEqualTo(request.getLieu());
        assertThat(formation.getVille()).isEqualTo(request.getVille());
        assertThat(formation.getLienParticipation()).isEqualTo(request.getLienParticipation());

        // Vérification des champs ignorés
        assertThat(formation.getId()).isNull();
        assertThat(formation.getCreatedAt()).isNull();
        assertThat(formation.getUpdatedAt()).isNull();
        assertThat(formation.getVersion()).isNull();
        assertThat(formation.getSecteur()).isNull();
        assertThat(formation.getRegion()).isNull();
        assertThat(formation.getStatut()).isEqualTo(A_VENIR);
    }

    @Test
    void shouldMapCreateRequestToEntityWithOnlineModality() {
        CreateFormationRequest request = CreateFormationRequest.builder()
                .libelle("Formation en ligne")
                .formateurs("Formateur Test")
                .description("Formation de test en ligne")
                .dateFormation(LocalDate.of(2024, 6, 15))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .modalite(ModaliteFormation.EN_LIGNE)
                .nbParticipants(20)
                .lienParticipation("https://teams.microsoft.com/test")
                .secteurId(1L)
                .regionId(1L)
                .build();

        Formation formation = mapper.toEntity(request);

        assertThat(formation).isNotNull();
        assertThat(formation.getModalite()).isEqualTo(ModaliteFormation.EN_LIGNE);
        assertThat(formation.getLienParticipation()).isEqualTo("https://teams.microsoft.com/test");
        assertThat(formation.getLieu()).isNull();
        assertThat(formation.getVille()).isNull();
    }

    @Test
    void shouldMapEntityToDTO() {
        Formation formation = FormationFixtures.defaultFormationPresentiel();

        FormationDTO dto = mapper.toDTO(formation);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(formation.getId());
        assertThat(dto.getLibelle()).isEqualTo(formation.getLibelle());
        assertThat(dto.getFormateurs()).isEqualTo(formation.getFormateurs());
        assertThat(dto.getDescription()).isEqualTo(formation.getDescription());
        assertThat(dto.getDateFormation()).isEqualTo(formation.getDateFormation());
        assertThat(dto.getHeureDebut()).isEqualTo(formation.getHeureDebut());
        assertThat(dto.getHeureFin()).isEqualTo(formation.getHeureFin());
        assertThat(dto.getModalite()).isEqualTo(formation.getModalite());
        assertThat(dto.getNbParticipants()).isEqualTo(formation.getNbParticipants());
        assertThat(dto.getLieu()).isEqualTo(formation.getLieu());
        assertThat(dto.getVille()).isEqualTo(formation.getVille());
        assertThat(dto.getLienParticipation()).isEqualTo(formation.getLienParticipation());
        assertThat(dto.getStatut()).isEqualTo(formation.getStatut());

        // Vérification des champs calculés
        assertThat(dto.getNbParticipantsInscrits()).isEqualTo(5);
        assertThat(dto.getComplet()).isFalse();
    }

    @Test
    void shouldMapEntityToDTOWithCompleteFormation() {
        Formation formation = FormationFixtures.defaultFormationPresentiel();
        formation.setId(2L); // ID avec 30 participants inscrits

        FormationDTO dto = mapper.toDTO(formation);

        assertThat(dto).isNotNull();
        assertThat(dto.getNbParticipantsInscrits()).isEqualTo(30);
        assertThat(dto.getComplet()).isTrue();
    }

    @Test
    void shouldMapEntityListToDTOList() {
        Formation formation1 = FormationFixtures.defaultFormationPresentiel();
        Formation formation2 = FormationFixtures.tirageAuSortEnLigneRG();
        List<Formation> formations = Arrays.asList(formation1, formation2);

        List<FormationDTO> dtos = mapper.toDTOList(formations);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getId()).isEqualTo(formation1.getId());
        assertThat(dtos.get(1).getId()).isEqualTo(formation2.getId());
    }

    @Test
    void shouldUpdateEntityFromDTO() {
        Formation existingFormation = FormationFixtures.defaultFormationPresentiel();
        UpdateFormationRequest updateRequest = createDefaultFormationUpdateRequest();

        mapper.updateEntityFromDTO(updateRequest, existingFormation);

        assertThat(existingFormation.getLibelle()).isEqualTo(updateRequest.getLibelle());
        assertThat(existingFormation.getFormateurs()).isEqualTo(updateRequest.getFormateurs());
        assertThat(existingFormation.getNbParticipants()).isEqualTo(updateRequest.getNbParticipants());
        assertThat(existingFormation.getModalite()).isEqualTo(updateRequest.getModalite());
        assertThat(existingFormation.getLienParticipation()).isEqualTo(updateRequest.getLienParticipation());

        // Vérification des champs non mis à jour
        assertThat(existingFormation.getId()).isEqualTo(FormationFixtures.FORMATION_ID_1);
    }

    @Test
    void shouldUpdateEntityFromDTOWithNullValues() {
        Formation existingFormation = FormationFixtures.defaultFormationPresentiel();
        String originalFormateurs = existingFormation.getFormateurs();

        UpdateFormationRequest updateRequest = UpdateFormationRequest.builder()
                .libelle("Nouveau libellé")
                .build();

        mapper.updateEntityFromDTO(updateRequest, existingFormation);

        assertThat(existingFormation.getLibelle()).isEqualTo("Nouveau libellé");
        assertThat(existingFormation.getFormateurs()).isEqualTo(originalFormateurs);
    }

    @Test
    void shouldCountParticipantsWithValidId() {
        Integer count = mapper.countParticipants(1L);
        assertThat(count).isEqualTo(5);
    }

    @Test
    void shouldCountParticipantsWithNullId() {
        Integer count = mapper.countParticipants(null);
        assertThat(count).isZero();
    }

    @Test
    void shouldDetermineFormationIsComplete() {
        Formation formation = FormationFixtures.defaultFormationPresentiel();
        formation.setId(2L); // 30 participants inscrits
        formation.setNbParticipants(30);

        Boolean isComplete = mapper.isFormationComplete(formation);

        assertThat(isComplete).isTrue();
    }

    @Test
    void shouldDetermineFormationIsNotComplete() {
        Formation formation = FormationFixtures.defaultFormationPresentiel();
        formation.setId(1L); // 5 participants inscrits
        formation.setNbParticipants(30);

        Boolean isComplete = mapper.isFormationComplete(formation);

        assertThat(isComplete).isFalse();
    }

    @Test
    void shouldReturnFalseForNullFormation() {
        Boolean isComplete = mapper.isFormationComplete(null);
        assertThat(isComplete).isFalse();
    }

    @Test
    void shouldReturnFalseForFormationWithNullId() {
        Formation formation = FormationFixtures.defaultFormationPresentiel();
        formation.setId(null);

        Boolean isComplete = mapper.isFormationComplete(formation);

        assertThat(isComplete).isFalse();
    }

    @Test
    void shouldReturnFalseForFormationWithNullNbParticipants() {
        Formation formation = FormationFixtures.defaultFormationPresentiel();
        formation.setNbParticipants(null);

        Boolean isComplete = mapper.isFormationComplete(formation);

        assertThat(isComplete).isFalse();
    }
}