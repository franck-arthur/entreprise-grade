package com.enterprise.app.application.mapper;

import com.enterprise.app.application.dto.RegionDTO;
import com.enterprise.app.domain.model.Region;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RegionMapper {

    RegionDTO toDTO(Region region);

    Region toEntity(RegionDTO regionDTO);
}