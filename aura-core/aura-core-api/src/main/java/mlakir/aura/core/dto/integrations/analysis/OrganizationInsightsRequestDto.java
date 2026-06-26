package mlakir.aura.core.dto.integrations.analysis;

import java.util.List;
import mlakir.aura.core.dto.OrganizationInsightsReviewItemDto;

public record OrganizationInsightsRequestDto(
        String organizationName,
        List<OrganizationInsightsReviewItemDto> reviews
) {
}
