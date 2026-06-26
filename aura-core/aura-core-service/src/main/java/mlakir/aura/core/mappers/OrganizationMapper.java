package mlakir.aura.core.mappers;

import mlakir.aura.core.dto.CreateOrganizationRequestDto;
import mlakir.aura.core.dto.OrganizationResponseDto;
import mlakir.aura.core.dto.OrganizationShortResponseDto;
import mlakir.aura.core.dto.UpdateOrganizationRequestDto;
import mlakir.aura.core.models.OrganizationEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrganizationEntity toEntity(CreateOrganizationRequestDto dto);

    OrganizationResponseDto toDto(OrganizationEntity entity);

    OrganizationShortResponseDto toShortDto(OrganizationEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateOrganizationRequestDto dto, @MappingTarget OrganizationEntity entity);
}
