package mlakir.aura.core.dto.integrations.analysis;

import java.math.BigDecimal;
import java.util.List;

public record BatchAnalyzeItemResponseDto(
        String sentiment,
        String topic,
        List<String> keywords,
        BigDecimal confidence,
        String modelVersion
) {
}
