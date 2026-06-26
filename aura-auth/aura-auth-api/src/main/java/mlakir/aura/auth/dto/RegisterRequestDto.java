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
    @Size(min = 3, message = "Login must contain at least 3 characters")
    @Schema(description = "Логин пользователя")
    private String login;

    @NotBlank
    @Size(min = 6, message = "Password must contain at least 6 characters")
    @Schema(description = "Пароль пользователя")
    private String password;

}
