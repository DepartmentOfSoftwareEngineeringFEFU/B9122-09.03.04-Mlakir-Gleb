package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import mlakir.aura.core.enums.CollectionJobStatus;

@Schema(description = "Collection job response")
public record CollectionJobResponseDto(
        Long id,
        Long sourceId,
        String sourceName,
        CollectionJobStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Integer collectedCount,
        String errorMessage,
        String triggeredBy
) {
}
