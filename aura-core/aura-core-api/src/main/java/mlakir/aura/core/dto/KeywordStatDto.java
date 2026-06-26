package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Keyword usage statistic")
public record KeywordStatDto(
        String keyword,
        long count
) {
}
