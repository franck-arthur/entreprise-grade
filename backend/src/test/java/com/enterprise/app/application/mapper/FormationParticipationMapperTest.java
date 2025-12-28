package com.enterprise.app.application.mapper;

import com.enterprise.app.application.dto.FormationParticipationDTO;
import com.enterprise.app.domain.model.*;
import com.enterprise.app.testing.fixtures.FormationParticipationFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class FormationParticipationMapperTest {

    private final FormationParticipationMapper mapper = Mappers.getMapper(FormationParticipationMapper.class);

    private FormationParticipation participation;
    private Long formationId;

    @BeforeEach
    void setUp() {
        formationId = 1L;
        participation = FormationParticipationFixtures.defaultParticipation();
    }

    @Test
    void shouldMapEntityToDTO() {
        // When
        FormationParticipationDTO dto = mapper.toDTO(participation);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(participation.getId());
        assertThat(dto.getFormationId()).isEqualTo(formationId);
        assertThat(dto.getUserId()).isEqualTo(participation.getUser().getId());
        assertThat(dto.getStatutParticipation()).isEqualTo(StatutParticipation.ABSENT);
        assertThat(dto.getDateInscription()).isEqualTo(participation.getDateInscription());

        // Formation details in DTO
        assertThat(dto.getFormationLibelle()).isEqualTo(participation.getFormation().getLibelle());

        // User details in DTO
        assertThat(dto.getUserEmail()).isEqualTo(participation.getUser().getEmail());
    }

    @Test
    void shouldReturnNull_WhenEntityIsNull() {
        // When
        FormationParticipationDTO dto = mapper.toDTO(null);

        // Then
        assertThat(dto).isNull();
    }

}