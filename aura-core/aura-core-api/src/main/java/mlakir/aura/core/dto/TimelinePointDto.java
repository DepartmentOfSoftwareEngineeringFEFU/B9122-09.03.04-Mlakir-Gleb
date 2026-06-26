package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dashboard timeline point")
public record TimelinePointDto(
        String month,
        long count
) {
}
