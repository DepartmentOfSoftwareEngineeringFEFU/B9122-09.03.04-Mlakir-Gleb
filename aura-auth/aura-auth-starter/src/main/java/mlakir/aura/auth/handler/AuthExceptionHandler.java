package mlakir.aura.auth.handler;

import jakarta.servlet.http.*;
import lombok.*;
import lombok.extern.slf4j.*;
import mlakir.aura.auth.*;
import mlakir.aura.auth.handler.strategy.*;
import mlakir.aura.exception.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authorization.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.web.bind.annotation.*;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class AuthExceptionHandler {

    private final AuthorizationDeniedResolver resolver;

    private final AuraExceptionHandler auraExceptionHandler;

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(
        BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Authentication failed: path={}, reason=invalid_credentials",
            request.getRequestURI());
        return auraExceptionHandler.handleAuraException(
            AuthExceptionFabric.createInvalidCredentials(),
            request
        );
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAccessDenied(
        AuthorizationDeniedException ex, HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.warn("Authorization denied: path={}, authenticated={}",
            request.getRequestURI(),
            auth != null && auth.isAuthenticated());
        return auraExceptionHandler.handleAuraException(
            resolver.resolve(auth).buildException(),
            request
        );
    }

}
