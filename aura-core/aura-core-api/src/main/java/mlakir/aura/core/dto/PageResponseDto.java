package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated response")
public record PageResponseDto<T>(
        @Schema(description = "Page content")
        List<T> content,
        @Schema(description = "Current page number", example = "0")
        int page,
        @Schema(description = "Page size", example = "20")
        int size,
        @Schema(description = "Total number of elements", example = "42")
        long totalElements,
        @Schema(description = "Total number of pages", example = "3")
        int totalPages,
        @Schema(description = "Whether current page is the last one", example = "false")
        boolean last
) {
}
