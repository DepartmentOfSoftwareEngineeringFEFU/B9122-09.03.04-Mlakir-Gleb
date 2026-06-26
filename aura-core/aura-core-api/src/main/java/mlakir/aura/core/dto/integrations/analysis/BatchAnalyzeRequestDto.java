package mlakir.aura.core.dto.integrations.analysis;

import java.util.List;

public record BatchAnalyzeRequestDto(
        List<BatchAnalyzeItemRequestDto> items
) {
}
