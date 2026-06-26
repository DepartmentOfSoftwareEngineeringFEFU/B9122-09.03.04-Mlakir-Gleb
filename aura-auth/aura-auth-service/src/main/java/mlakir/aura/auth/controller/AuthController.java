package mlakir.aura.auth.controller;

import lombok.*;
import mlakir.aura.auth.api.*;
import mlakir.aura.auth.dto.*;
import mlakir.aura.auth.service.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        return authService.login(request);
    }

    @Override
    public AuthResponseDto register(RegisterRequestDto request) {
        return authService.register(request);
    }

    @Override
    public AuthResponseDto refresh(TokenRequestDto request) {
        return authService.refresh(request);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void logout(String authorizationHeader) {
        authService.logout(authorizationHeader);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void validateAccessToken() {
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void logoutAll() {
        authService.logoutAll();
    }

}
