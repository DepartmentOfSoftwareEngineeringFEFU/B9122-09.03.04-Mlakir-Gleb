package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dashboard summary")
public record DashboardSummaryDto(
        long totalReviews,
        long positiveCount,
        long neutralCount,
        long negativeCount
) {
}
