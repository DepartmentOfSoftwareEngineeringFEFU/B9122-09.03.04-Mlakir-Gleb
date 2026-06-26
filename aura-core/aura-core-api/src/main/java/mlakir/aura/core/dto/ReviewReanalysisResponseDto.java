package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Review reanalysis result")
public record ReviewReanalysisResponseDto(
        Long organizationId,
        Long sourceId,
        int requestedCount,
        int reanalyzedCount,
        int failedCount,
        int skippedCount,
        String errorMessage
) {
}
