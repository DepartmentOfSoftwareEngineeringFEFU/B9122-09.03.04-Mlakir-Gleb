package mlakir.aura.auth.dto;

import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.constraints.*;
import lombok.*;

import static lombok.AccessLevel.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
@Schema(description = "Запрос регистрации пользователя")
public class RegisterRequestDto {

    @NotBlank
    @Schema(description = "Логин пользователя")
    private String login;

    @NotBlank
    @Schema(description = "Пароль пользователя")
    private String password;

}
