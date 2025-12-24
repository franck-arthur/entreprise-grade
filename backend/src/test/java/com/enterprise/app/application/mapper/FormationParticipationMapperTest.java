package com.enterprise.app.application.mapper;

import com.enterprise.app.application.dto.FormationParticipationDTO;
import com.enterprise.app.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FormationParticipationMapperTest {

    private final FormationParticipationMapper mapper = Mappers.getMapper(FormationParticipationMapper.class);

    private Formation formation;
    private User user;
    private FormationParticipation participation;
    private UUID formationId;
    private UUID userId;
    private UUID participationId;

    @BeforeEach
    void setUp() {
        formationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        participationId = UUID.randomUUID();

        formation = Formation.builder()
                .id(formationId)
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .description("Description test")
                .dateFormation(LocalDate.now().plusDays(7))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("Île-de-France")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .build();

        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .active(true)
                .build();

        participation = FormationParticipation.builder()
                .id(participationId)
                .formation(formation)
                .user(user)
                .statutParticipation(StatutParticipation.ABSENT)
                .dateInscription(LocalDateTime.now())
                .datePresence(null)
                .commentaire("Inscription test")
                .build();
    }

    @Test
    void shouldMapEntityToDTO() {
        // When
        FormationParticipationDTO dto = mapper.toDTO(participation);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(participationId);
        assertThat(dto.getFormationId()).isEqualTo(formationId);
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.getStatutParticipation()).isEqualTo(StatutParticipation.ABSENT);
        assertThat(dto.getDateInscription()).isEqualTo(participation.getDateInscription());
        assertThat(dto.getDatePresence()).isNull();
        assertThat(dto.getCommentaire()).isEqualTo("Inscription test");

        // Formation details in DTO
        assertThat(dto.getFormationLibelle()).isEqualTo("Formation Test");
        assertThat(dto.getFormationDate()).isEqualTo(LocalDate.now().plusDays(7));
        assertThat(dto.getFormationHeureDebut()).isEqualTo(LocalTime.of(9, 0));
        assertThat(dto.getFormationHeureFin()).isEqualTo(LocalTime.of(17, 0));
        assertThat(dto.getFormationModalite()).isEqualTo(ModaliteFormation.PRESENTIEL);
        assertThat(dto.getFormationLieu()).isEqualTo("Paris");
        assertThat(dto.getFormationVille()).isEqualTo("Paris");

        // User details in DTO
        assertThat(dto.getUserUsername()).isEqualTo("testuser");
        assertThat(dto.getUserFirstName()).isEqualTo("Test");
        assertThat(dto.getUserLastName()).isEqualTo("User");
        assertThat(dto.getUserEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldMapEntityToDTO_WhenPresent() {
        // Given
        FormationParticipation presentParticipation = participation.toBuilder()
                .statutParticipation(StatutParticipation.PRESENT)
                .datePresence(LocalDateTime.now())
                .commentaire("Présent toute la journée")
                .build();

        // When
        FormationParticipationDTO dto = mapper.toDTO(presentParticipation);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getStatutParticipation()).isEqualTo(StatutParticipation.PRESENT);
        assertThat(dto.getDatePresence()).isNotNull();
        assertThat(dto.getCommentaire()).isEqualTo("Présent toute la journée");
    }

    @Test
    void shouldMapEntityToDTO_WhenAbsent() {
        // Given
        FormationParticipation absentParticipation = participation.toBuilder()
                .statutParticipation(StatutParticipation.ABSENT)
                .datePresence(null)
                .commentaire("Absent pour raisons personnelles")
                .build();

        // When
        FormationParticipationDTO dto = mapper.toDTO(absentParticipation);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getStatutParticipation()).isEqualTo(StatutParticipation.ABSENT);
        assertThat(dto.getDatePresence()).isNull();
        assertThat(dto.getCommentaire()).isEqualTo("Absent pour raisons personnelles");
    }

    @Test
    void shouldMapEntityToDTO_WhenFormationEnLigne() {
        // Given
        Formation formationEnLigne = formation.toBuilder()
                .modalite(ModaliteFormation.EN_LIGNE)
                .lieu(null)
                .ville(null)
                .lienParticipation("https://example.com/formation")
                .build();

        FormationParticipation participationEnLigne = participation.toBuilder()
                .formation(formationEnLigne)
                .build();

        // When
        FormationParticipationDTO dto = mapper.toDTO(participationEnLigne);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getFormationModalite()).isEqualTo(ModaliteFormation.EN_LIGNE);
        assertThat(dto.getFormationLieu()).isNull();
        assertThat(dto.getFormationVille()).isNull();
        assertThat(dto.getFormationLienParticipation()).isEqualTo("https://example.com/formation");
    }

    @Test
    void shouldHandleNullValues() {
        // Given
        FormationParticipation participationWithNulls = FormationParticipation.builder()
                .id(participationId)
                .formation(formation)
                .user(user)
                .statutParticipation(StatutParticipation.ABSENT)
                .dateInscription(LocalDateTime.now())
                .datePresence(null)
                .commentaire(null)
                .build();

        // When
        FormationParticipationDTO dto = mapper.toDTO(participationWithNulls);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getDatePresence()).isNull();
        assertThat(dto.getCommentaire()).isNull();
    }

    @Test
    void shouldMapEntityToDTO_WithMinimalData() {
        // Given - Participation with minimal required data
        FormationParticipation minimalParticipation = FormationParticipation.builder()
                .id(participationId)
                .formation(formation)
                .user(user)
                .statutParticipation(StatutParticipation.ABSENT)
                .dateInscription(LocalDateTime.now())
                .build();

        // When
        FormationParticipationDTO dto = mapper.toDTO(minimalParticipation);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(participationId);
        assertThat(dto.getFormationId()).isEqualTo(formationId);
        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.getStatutParticipation()).isEqualTo(StatutParticipation.ABSENT);
        assertThat(dto.getDateInscription()).isNotNull();
    }

    @Test
    void shouldMapEntityToDTO_WhenFormationHybride() {
        // Given
        Formation formationHybride = formation.toBuilder()
                .modalite(ModaliteFormation.HYBRIDE)
                .lieu("Salle 101")
                .ville("Paris")
                .lienParticipation("https://example.com/hybrid")
                .build();

        FormationParticipation participationHybride = participation.toBuilder()
                .formation(formationHybride)
                .build();

        // When
        FormationParticipationDTO dto = mapper.toDTO(participationHybride);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getFormationModalite()).isEqualTo(ModaliteFormation.HYBRIDE);
        assertThat(dto.getFormationLieu()).isEqualTo("Salle 101");
        assertThat(dto.getFormationVille()).isEqualTo("Paris");
        assertThat(dto.getFormationLienParticipation()).isEqualTo("https://example.com/hybrid");
    }

    @Test
    void shouldReturnNull_WhenEntityIsNull() {
        // When
        FormationParticipationDTO dto = mapper.toDTO(null);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    void shouldMapUserDetailsCorrectly() {
        // Given
        User userWithAllDetails = User.builder()
                .id(userId)
                .username("john.doe")
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .active(true)
                .build();

        FormationParticipation participationWithDetailedUser = participation.toBuilder()
                .user(userWithAllDetails)
                .build();

        // When
        FormationParticipationDTO dto = mapper.toDTO(participationWithDetailedUser);

        // Then
        assertThat(dto.getUserUsername()).isEqualTo("john.doe");
        assertThat(dto.getUserFirstName()).isEqualTo("John");
        assertThat(dto.getUserLastName()).isEqualTo("Doe");
        assertThat(dto.getUserEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void shouldMapFormationDetailsCorrectly() {
        // Given
        Formation detailedFormation = Formation.builder()
                .id(formationId)
                .libelle("Formation Avancée Java Spring")
                .formateurs("Expert Java, Maître Spring")
                .description("Formation approfondie sur Spring Framework")
                .dateFormation(LocalDate.of(2024, 6, 15))
                .heureDebut(LocalTime.of(8, 30))
                .heureFin(LocalTime.of(18, 30))
                .secteur("Informatique")
                .region("Auvergne-Rhône-Alpes")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(15)
                .lieu("Centre de Formation IT")
                .ville("Lyon")
                .build();

        FormationParticipation participationWithDetailedFormation = participation.toBuilder()
                .formation(detailedFormation)
                .build();

        // When
        FormationParticipationDTO dto = mapper.toDTO(participationWithDetailedFormation);

        // Then
        assertThat(dto.getFormationLibelle()).isEqualTo("Formation Avancée Java Spring");
        assertThat(dto.getFormationDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(dto.getFormationHeureDebut()).isEqualTo(LocalTime.of(8, 30));
        assertThat(dto.getFormationHeureFin()).isEqualTo(LocalTime.of(18, 30));
        assertThat(dto.getFormationModalite()).isEqualTo(ModaliteFormation.PRESENTIEL);
        assertThat(dto.getFormationLieu()).isEqualTo("Centre de Formation IT");
        assertThat(dto.getFormationVille()).isEqualTo("Lyon");
    }
}