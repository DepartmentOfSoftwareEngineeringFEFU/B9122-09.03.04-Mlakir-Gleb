package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Organization short response")
public record OrganizationShortResponseDto(
        Long id,
        String name,
        String shortName
) {
}
