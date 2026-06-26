package mlakir.aura.core.repositories;

import java.time.OffsetDateTime;
import mlakir.aura.core.enums.SentimentType;

public record DashboardFilters(
        Long organizationId,
        OffsetDateTime from,
        OffsetDateTime to,
        Long sourceId,
        SentimentType sentiment
) {
}
