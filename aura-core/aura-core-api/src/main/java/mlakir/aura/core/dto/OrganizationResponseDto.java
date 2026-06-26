package mlakir.aura.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Organization response")
public record OrganizationResponseDto(
        Long id,
        String name,
        String shortName,
        String description,
        String website,
        @JsonProperty("isActive")
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
