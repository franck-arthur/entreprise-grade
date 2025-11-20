package com.enterprise.app.application.mapper;

import com.enterprise.app.application.dto.CreateUserRequest;
import com.enterprise.app.application.dto.UpdateUserRequest;
import com.enterprise.app.application.dto.UserDTO;
import com.enterprise.app.domain.model.User;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for User entity and DTOs.
 *
 * MapStruct automatically generates the implementation at compile time.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    /**
     * Map User entity to UserDTO.
     */
    UserDTO toDTO(User user);

    /**
     * Map list of User entities to list of UserDTOs.
     */
    List<UserDTO> toDTOList(List<User> users);

    /**
     * Map CreateUserRequest to User entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "emailVerified", constant = "false")
    User toEntity(CreateUserRequest request);

    /**
     * Update existing User entity from UpdateUserRequest.
     * Only updates non-null fields from the request.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)  // Username cannot be changed
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    void updateEntityFromDTO(UpdateUserRequest request, @MappingTarget User user);
}
