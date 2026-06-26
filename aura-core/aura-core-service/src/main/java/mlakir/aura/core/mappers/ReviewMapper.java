package mlakir.aura.core.mappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import mlakir.aura.core.dto.ReviewAnalysisDto;
import mlakir.aura.core.dto.ReviewListItemDto;
import mlakir.aura.core.dto.ReviewResponseDto;
import mlakir.aura.core.models.ReviewAnalysisEntity;
import mlakir.aura.core.models.ReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "sourceId", source = "source.id")
    @Mapping(target = "sourceName", source = "source.name")
    ReviewListItemDto toListItemDto(ReviewEntity entity);

    @Mapping(target = "sourceId", source = "source.id")
    @Mapping(target = "sourceName", source = "source.name")
    ReviewResponseDto toResponseDto(ReviewEntity entity);

    @Mapping(target = "keywords", expression = "java(splitKeywords(entity.getKeywords()))")
    ReviewAnalysisDto toAnalysisDto(ReviewAnalysisEntity entity);

    default List<String> splitKeywords(String rawKeywords) {
        if (rawKeywords == null || rawKeywords.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(rawKeywords.split("\\s*,\\s*"))
                .filter(value -> !value.isBlank())
                .toList();
    }
}
