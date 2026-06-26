package mlakir.aura.core.mappers;

import mlakir.aura.core.dto.CollectionJobResponseDto;
import mlakir.aura.core.models.CollectionJobEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CollectionJobMapper {

    @Mapping(target = "sourceId", source = "source.id")
    @Mapping(target = "sourceName", source = "source.name")
    CollectionJobResponseDto toDto(CollectionJobEntity entity);
}
