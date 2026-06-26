package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import mlakir.aura.core.enums.ReviewStatus;

@Schema(description = "Review details")
public record ReviewResponseDto(
        Long id,
        Long sourceId,
        String sourceName,
        String externalId,
        String text,
        String authorName,
        Integer rating,
        OffsetDateTime publishedAt,
        String originalUrl,
        OffsetDateTime collectedAt,
        ReviewStatus status,
        ReviewAnalysisDto analysis
) {
}
