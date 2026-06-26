package mlakir.aura.core.dto.integrations.analysis;

import java.util.List;

public record OrganizationInsightsAnalysisResponseDto(
        String summary,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations,
        String modelVersion
) {
}
