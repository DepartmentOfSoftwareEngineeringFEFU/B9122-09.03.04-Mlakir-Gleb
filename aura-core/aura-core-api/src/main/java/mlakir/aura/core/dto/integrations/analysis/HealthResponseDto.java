package mlakir.aura.core.dto.integrations.analysis;

public record HealthResponseDto(
        String status,
        String mode,
        String modelVersion
) {
}
