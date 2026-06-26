package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Organization AI insights report")
public record OrganizationInsightsResponseDto(
        Long organizationId,
        String organizationName,
        String summary,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations,
        OffsetDateTime generatedAt,
        String modelVersion,
        boolean cached,
        int reviewsUsed
) {
}
