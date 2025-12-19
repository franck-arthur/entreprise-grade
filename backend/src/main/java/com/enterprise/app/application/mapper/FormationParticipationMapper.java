package com.enterprise.app.application.mapper;

import com.enterprise.app.application.dto.FormationParticipationDTO;
import com.enterprise.app.domain.model.FormationParticipation;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for FormationParticipation entity and DTOs.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface FormationParticipationMapper {

    /**
     * Map FormationParticipation entity to FormationParticipationDTO.
     */
    @Mapping(target = "formationId", source = "formation.id")
    @Mapping(target = "formationLibelle", source = "formation.libelle")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFullName", source = "user.fullName")
    @Mapping(target = "userEmail", source = "user.email")
    FormationParticipationDTO toDTO(FormationParticipation participation);

    /**
     * Map list of FormationParticipation entities to list of FormationParticipationDTOs.
     */
    List<FormationParticipationDTO> toDTOList(List<FormationParticipation> participations);
}