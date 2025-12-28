package com.enterprise.app.application.mapper;

import com.enterprise.app.application.dto.CreateFormationRequest;
import com.enterprise.app.application.dto.FormationDTO;
import com.enterprise.app.application.dto.UpdateFormationRequest;
import com.enterprise.app.domain.model.Formation;
import com.enterprise.app.domain.repository.FormationParticipationRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * MapStruct mapper for Formation entity and DTOs.
 * Les DTOs peuvent maintenant être retournés directement depuis les repositories.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = {SecteurMapper.class, RegionMapper.class}
)
public abstract class FormationMapper {

    @Autowired
    protected FormationParticipationRepository participationRepository;

    /**
     * Map Formation entity to FormationDTO (pour les opérations CRUD standards).
     */
    @Mapping(target = "nbParticipantsInscrits", expression = "java(countParticipants(formation.getId()))")
    @Mapping(target = "complet", expression = "java(isFormationComplete(formation))")
    public abstract FormationDTO toDTO(Formation formation);

    /**
     * Map list of Formations to list of FormationDTOs.
     */
    public abstract List<FormationDTO> toDTOList(List<Formation> formations);

    /**
     * Map CreateFormationRequest to Formation entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "secteur", ignore = true)
    @Mapping(target = "region", ignore = true)
    public abstract Formation toEntity(CreateFormationRequest request);

    /**
     * Update existing Formation entity from UpdateFormationRequest.
     * Only updates non-null fields from the request.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "secteur", ignore = true)
    @Mapping(target = "region", ignore = true)
    public abstract void updateEntityFromDTO(UpdateFormationRequest request, @MappingTarget Formation formation);

    /**
     * Compte le nombre de participants inscrits pour une formation.
     */
    protected Integer countParticipants(Long formationId) {
        if (formationId == null) {
            return 0;
        }
        return Math.toIntExact(participationRepository.countByFormationId(formationId));
    }

    /**
     * Détermine si une formation est complète.
     */
    protected Boolean isFormationComplete(Formation formation) {
        if (formation == null || formation.getId() == null || formation.getNbParticipants() == null) {
            return false;
        }
        Integer nbInscrits = countParticipants(formation.getId());
        return nbInscrits >= formation.getNbParticipants();
    }
}