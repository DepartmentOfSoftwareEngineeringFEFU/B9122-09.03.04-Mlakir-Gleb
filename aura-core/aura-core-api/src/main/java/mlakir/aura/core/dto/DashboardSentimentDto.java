package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dashboard sentiment breakdown")
public record DashboardSentimentDto(
        long positive,
        long neutral,
        long negative
) {
}
