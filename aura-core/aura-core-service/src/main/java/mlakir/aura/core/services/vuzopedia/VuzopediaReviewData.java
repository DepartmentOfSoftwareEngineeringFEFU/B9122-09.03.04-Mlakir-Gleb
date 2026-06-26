package mlakir.aura.core.services.vuzopedia;

import java.time.OffsetDateTime;

public record VuzopediaReviewData(
        String externalId,
        String text,
        OffsetDateTime publishedAt,
        String authorName,
        VuzopediaSentimentHint sentimentHint
) {
}
