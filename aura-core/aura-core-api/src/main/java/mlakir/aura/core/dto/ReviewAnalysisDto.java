package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.enums.ReviewTopic;
import mlakir.aura.core.enums.SentimentType;

@Schema(description = "Review analysis result")
public record ReviewAnalysisDto(
        SentimentType sentiment,
        ReviewTopic topic,
        List<String> keywords,
        BigDecimal confidence,
        String modelVersion,
        OffsetDateTime analyzedAt
) {
}
