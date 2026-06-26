package mlakir.aura.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(description = "Update organization request")
public record UpdateOrganizationRequestDto(
        @Size(max = 255)
        String name,
        @Size(max = 100)
        String shortName,
        @Size(max = 2000)
        String description,
        @URL
        @Size(max = 1000)
        String website,
        Boolean isActive
) {
}
