package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(description = "Create organization request")
public record CreateOrganizationRequestDto(
        @NotBlank
        @Size(max = 255)
        String name,
        @NotBlank
        @Size(max = 100)
        String shortName,
        @Size(max = 2000)
        String description,
        @URL
        @Size(max = 1000)
        String website
) {
}
