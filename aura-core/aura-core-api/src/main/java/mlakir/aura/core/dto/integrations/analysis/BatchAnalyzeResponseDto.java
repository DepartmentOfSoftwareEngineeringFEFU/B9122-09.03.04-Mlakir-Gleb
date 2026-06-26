package mlakir.aura.core.dto.integrations.analysis;

import java.util.List;

public record BatchAnalyzeResponseDto(
        List<BatchAnalyzeItemResponseDto> items
) {
}
