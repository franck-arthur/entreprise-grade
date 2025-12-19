package com.enterprise.app.application.mapper;

import com.enterprise.app.application.dto.CreateFormationRequest;
import com.enterprise.app.application.dto.FormationDTO;
import com.enterprise.app.application.dto.UpdateFormationRequest;
import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.infrastructure.persistence.projection.FormationProjection;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for Formation entity and DTOs.
 * Utilise les projections pour optimiser les requêtes avec comptage des participants.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface FormationMapper {

    /**
     * Map Formation entity to FormationDTO.
     * Note: Utilise FormationProjection pour éviter les requêtes N+1.
     */
    @Mapping(target = "complet", expression = "java(projection.isComplet())")
    FormationDTO toDTO(FormationProjection projection);

    /**
     * Map Formation entity to FormationDTO (fallback sans projection).
     * @deprecated Préférer la méthode avec projection pour les performances.
     */
    @Deprecated
    @Mapping(target = "nbParticipantsInscrits", ignore = true)
    @Mapping(target = "complet", ignore = true)
    FormationDTO toDTO(Formation formation);

    /**
     * Map list of FormationProjections to list of FormationDTOs.
     */
    List<FormationDTO> toDTOList(List<FormationProjection> projections);

    /**
     * Map CreateFormationRequest to Formation entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Formation toEntity(CreateFormationRequest request);

    /**
     * Update existing Formation entity from UpdateFormationRequest.
     * Only updates non-null fields from the request.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDTO(UpdateFormationRequest request, @MappingTarget Formation formation);
}