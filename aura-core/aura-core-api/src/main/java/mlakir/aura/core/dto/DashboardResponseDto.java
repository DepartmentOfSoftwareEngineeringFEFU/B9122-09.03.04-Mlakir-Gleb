package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Aggregated dashboard response")
public record DashboardResponseDto(
        long totalReviews,
        long sourcesCount,
        DashboardSentimentDto sentiment,
        List<CategoryStatDto> topCategories,
        List<TimelinePointDto> timeline
) {
}
