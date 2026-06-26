package mlakir.aura.auth.handler;

import jakarta.servlet.http.*;
import lombok.*;
import lombok.extern.slf4j.*;
import mlakir.aura.auth.*;
import org.springframework.security.core.*;
import org.springframework.security.web.*;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailResponseWriter responseWriter;

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) {
        log.warn("Unauthorized request: path={}, reason=invalid_token",
            request.getRequestURI());
        responseWriter.write(request, response, AuthExceptionFabric.createInvalidToken());
    }

}
