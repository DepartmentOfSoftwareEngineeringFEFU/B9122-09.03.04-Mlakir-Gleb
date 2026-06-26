package mlakir.aura.auth.dto;

import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.constraints.*;
import lombok.*;

import static lombok.AccessLevel.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
@Schema(description = "Запрос для передачи токена(access или refresh)")
public class TokenRequestDto {

    @NotBlank
    @Schema(description = "Токен")
    private String token;

}
