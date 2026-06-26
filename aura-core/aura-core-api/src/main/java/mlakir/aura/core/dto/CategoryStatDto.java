package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mlakir.aura.core.enums.ReviewTopic;

@Schema(description = "Category statistics item")
public record CategoryStatDto(
        ReviewTopic category,
        long count
) {
}
