package mlakir.aura.auth.api;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.*;
import jakarta.validation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import mlakir.aura.auth.dto.*;

@Tag(name = "Авторизация")
public interface AuthApi {

    @PostMapping("/auth/login")
    @Operation(summary = "Аутентификация пользователя")
    AuthResponseDto login(@Valid @RequestBody LoginRequestDto request);

    @PostMapping("/auth/register")
    @Operation(summary = "Регистрация пользователя")
    AuthResponseDto register(@Valid @RequestBody RegisterRequestDto request);

    @PostMapping("/auth/refresh")
    @Operation(summary = "Обновление пары JWT токенов")
    AuthResponseDto refresh(@Valid @RequestBody TokenRequestDto request);

    @PostMapping("/auth/logout")
    @Operation(summary = "Удаление текущей активной сессии по access token из Authorization header")
    void logout(
        @Parameter(hidden = true)
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    );

    @GetMapping("/auth/validate")
    @Operation(summary = "Проверка валидности access token из Authorization header")
    void validateAccessToken();

    @PostMapping("/auth/logout/all")
    @Operation(summary = "Удаление всех активных сессий пользователя")
    void logoutAll();

}
