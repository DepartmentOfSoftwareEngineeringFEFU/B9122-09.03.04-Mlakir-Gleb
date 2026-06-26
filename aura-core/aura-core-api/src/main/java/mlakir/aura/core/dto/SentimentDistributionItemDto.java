package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mlakir.aura.core.enums.SentimentType;

@Schema(description = "Sentiment distribution item")
public record SentimentDistributionItemDto(
        SentimentType sentiment,
        long count
) {
}
