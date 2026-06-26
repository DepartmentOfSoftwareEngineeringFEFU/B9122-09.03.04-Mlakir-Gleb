package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Manual CSV import result")
public record ManualImportResponseDto(
        Long sourceId,
        String fileName,
        int totalRows,
        int importedCount,
        int duplicateCount,
        int invalidCount
) {
}
