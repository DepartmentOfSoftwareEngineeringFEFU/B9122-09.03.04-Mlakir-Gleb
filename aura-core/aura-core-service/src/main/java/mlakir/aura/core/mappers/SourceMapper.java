package mlakir.aura.core.mappers;

import mlakir.aura.core.dto.CreateSourceRequestDto;
import mlakir.aura.core.dto.SourceResponseDto;
import mlakir.aura.core.models.SourceEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = OrganizationMapper.class)
public interface SourceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastCollectedAt", ignore = true)
    @Mapping(target = "nextCollectionAt", ignore = true)
    SourceEntity toEntity(CreateSourceRequestDto dto);

    SourceResponseDto toDto(SourceEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastCollectedAt", ignore = true)
    @Mapping(target = "nextCollectionAt", ignore = true)
    void updateEntity(mlakir.aura.core.dto.UpdateSourceRequestDto dto, @MappingTarget SourceEntity entity);
}
