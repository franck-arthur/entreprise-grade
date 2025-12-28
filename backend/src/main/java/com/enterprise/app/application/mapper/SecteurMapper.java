package com.enterprise.app.application.mapper;

import com.enterprise.app.application.dto.SecteurDTO;
import com.enterprise.app.domain.model.Secteur;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SecteurMapper {

    SecteurDTO toDTO(Secteur secteur);

    Secteur toEntity(SecteurDTO secteurDTO);
}