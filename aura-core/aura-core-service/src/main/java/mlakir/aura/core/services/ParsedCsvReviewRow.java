package mlakir.aura.core.services;

import java.time.OffsetDateTime;

public record ParsedCsvReviewRow(
        String externalId,
        String text,
        String authorName,
        OffsetDateTime publishedAt,
        String originalUrl,
        Integer rating
) {
}
