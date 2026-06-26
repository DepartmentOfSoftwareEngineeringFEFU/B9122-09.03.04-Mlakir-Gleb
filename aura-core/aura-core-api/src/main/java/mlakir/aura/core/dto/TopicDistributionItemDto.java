package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mlakir.aura.core.enums.ReviewTopic;

@Schema(description = "Topic distribution item")
public record TopicDistributionItemDto(
        ReviewTopic topic,
        long count
) {
}
