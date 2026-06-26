package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Review summary details")
public record ReviewSummaryResponseDto(
        Long reviewId,
        String summary,
        OffsetDateTime generatedAt,
        String modelVersion,
        boolean cached
) {
}
