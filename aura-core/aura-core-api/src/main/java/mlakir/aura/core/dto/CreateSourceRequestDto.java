package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mlakir.aura.core.enums.CollectionMode;
import mlakir.aura.core.enums.SourceType;

@Schema(description = "Create source request")
public record CreateSourceRequestDto(
        @NotNull
        Long organizationId,
        @NotBlank
        @Size(max = 255)
        String name,
        @NotNull
        @Schema(
                description = "Source type",
                allowableValues = {"MANUAL_IMPORT", "TABITURIENT", "OTZOVIK", "VUZOPEDIA"},
                example = "TABITURIENT"
        )
        SourceType type,
        @NotBlank
        @Size(max = 1000)
        @Schema(
                description = "Source base URL. For TABITURIENT use https://tabiturient.ru/vuzu/{slug}/",
                example = "https://tabiturient.ru/vuzu/dvfu/"
        )
        String baseUrl,
        @Deprecated
        CollectionMode collectionMode,
        Boolean scheduleEnabled,
        Integer scheduleIntervalMinutes,
        @Size(max = 2000)
        String description
) {
}
