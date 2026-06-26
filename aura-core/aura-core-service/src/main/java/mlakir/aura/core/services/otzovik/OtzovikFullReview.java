package mlakir.aura.core.services.otzovik;

import java.time.OffsetDateTime;

public record OtzovikFullReview(
        String title,
        String text,
        Integer rating,
        OffsetDateTime publishedAt,
        String authorName,
        String pros,
        String cons
) {
}
