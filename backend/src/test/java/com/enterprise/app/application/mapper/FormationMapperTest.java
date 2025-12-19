package com.enterprise.app.application.mapper;

import com.enterprise.app.application.dto.CreateFormationRequest;
import com.enterprise.app.application.dto.FormationDTO;
import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.model.ModaliteFormation;
import com.enterprise.app.infrastructure.persistence.projection.FormationProjection;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FormationMapperTest {

    private final FormationMapper mapper = Mappers.getMapper(FormationMapper.class);

    @Test
    void shouldMapCreateRequestToEntity() {
        CreateFormationRequest request = CreateFormationRequest.builder()
                .libelle("Formation Test")
                .formateurs("Formateur Test")
                .description("Description test")
                .dateFormation(LocalDate.now().plusDays(30))
                .heureDebut(LocalTime.of(9, 0))
                .heureFin(LocalTime.of(17, 0))
                .secteur("IT")
                .region("IDF")
                .modalite(ModaliteFormation.PRESENTIEL)
                .nbParticipants(20)
                .lieu("Paris")
                .ville("Paris")
                .lienParticipation("http://test.com")
                .build();

        Formation formation = mapper.toEntity(request);

        assertThat(formation).isNotNull();
        assertThat(formation.getLibelle()).isEqualTo("Formation Test");
        assertThat(formation.getFormateurs()).isEqualTo("Formateur Test");
        assertThat(formation.getModalite()).isEqualTo(ModaliteFormation.PRESENTIEL);
        assertThat(formation.getNbParticipants()).isEqualTo(20);
        assertThat(formation.getId()).isNull();
        assertThat(formation.getStatut()).isNull();
    }

    @Test
    void shouldMapProjectionToDTO() {
        FormationProjection projection = new FormationProjectionImpl();

        FormationDTO dto = mapper.toDTO(projection);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(projection.getId());
        assertThat(dto.getLibelle()).isEqualTo(projection.getLibelle());
        assertThat(dto.getNbParticipantsInscrits()).isEqualTo(projection.getNbParticipantsInscrits());
        assertThat(dto.isComplet()).isEqualTo(projection.isComplet());
    }

    private static class FormationProjectionImpl implements FormationProjection {
        @Override
        public UUID getId() { return UUID.randomUUID(); }
        @Override
        public String getLibelle() { return "Test Formation"; }
        @Override
        public String getFormateurs() { return "Test Formateurs"; }
        @Override
        public String getDescription() { return "Test Description"; }
        @Override
        public LocalDate getDateFormation() { return LocalDate.now(); }
        @Override
        public LocalTime getHeureDebut() { return LocalTime.of(9, 0); }
        @Override
        public LocalTime getHeureFin() { return LocalTime.of(17, 0); }
        @Override
        public String getSecteur() { return "IT"; }
        @Override
        public String getRegion() { return "IDF"; }
        @Override
        public ModaliteFormation getModalite() { return ModaliteFormation.PRESENTIEL; }
        @Override
        public Integer getNbParticipants() { return 20; }
        @Override
        public String getLieu() { return "Paris"; }
        @Override
        public String getVille() { return "Paris"; }
        @Override
        public String getLienParticipation() { return "http://test.com"; }
        @Override
        public com.enterprise.app.domain.model.FormationStatut getStatut() { return null; }
        @Override
        public java.time.LocalDateTime getCreatedAt() { return java.time.LocalDateTime.now(); }
        @Override
        public java.time.LocalDateTime getUpdatedAt() { return java.time.LocalDateTime.now(); }
        @Override
        public Integer getNbParticipantsInscrits() { return 15; }
    }
}