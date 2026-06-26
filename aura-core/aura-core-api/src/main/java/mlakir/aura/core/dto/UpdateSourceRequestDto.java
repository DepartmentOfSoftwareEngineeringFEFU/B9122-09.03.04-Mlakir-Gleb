package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import mlakir.aura.core.enums.CollectionMode;

@Schema(description = "Update source request")
public record UpdateSourceRequestDto(
        Long organizationId,
        @Size(max = 255)
        String name,
        @Size(max = 1000)
        @Schema(
                description = "Updated base URL. For TABITURIENT use https://tabiturient.ru/vuzu/{slug}/",
                example = "https://tabiturient.ru/vuzu/dvfu/"
        )
        String baseUrl,
        Boolean isActive,
        @Deprecated
        CollectionMode collectionMode,
        Boolean scheduleEnabled,
        Integer scheduleIntervalMinutes,
        @Size(max = 2000)
        String description
) {
}
