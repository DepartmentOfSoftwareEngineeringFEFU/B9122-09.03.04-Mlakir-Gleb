package mlakir.aura.core.services;

import java.time.OffsetDateTime;

public record ReviewCandidate(
        String externalId,
        String text,
        String authorName,
        Integer rating,
        OffsetDateTime publishedAt,
        String originalUrl
) {
}
