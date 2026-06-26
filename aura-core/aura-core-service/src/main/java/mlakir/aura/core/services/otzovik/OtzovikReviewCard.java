package mlakir.aura.core.services.otzovik;

import java.time.OffsetDateTime;

public record OtzovikReviewCard(
        String externalId,
        String reviewUrl,
        String title,
        String teaser,
        Integer rating,
        OffsetDateTime publishedAt,
        String authorName,
        String pros,
        String cons,
        Integer likesCount,
        Integer commentsCount
) {
}
