package mlakir.aura.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import mlakir.aura.core.enums.CollectionMode;
import mlakir.aura.core.enums.SourceType;

@Schema(description = "Source response")
public record SourceResponseDto(
        Long id,
        OrganizationShortResponseDto organization,
        String name,
        SourceType type,
        String baseUrl,
        @JsonProperty("isActive")
        Boolean isActive,
        @Deprecated
        CollectionMode collectionMode,
        Boolean scheduleEnabled,
        Integer scheduleIntervalMinutes,
        OffsetDateTime lastCollectedAt,
        OffsetDateTime nextCollectionAt,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
