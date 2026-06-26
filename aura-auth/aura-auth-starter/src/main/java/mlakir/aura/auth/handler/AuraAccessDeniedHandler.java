package mlakir.aura.auth.handler;

import jakarta.servlet.http.*;
import lombok.*;
import lombok.extern.slf4j.*;
import mlakir.aura.auth.*;
import org.springframework.security.access.*;
import org.springframework.security.web.access.*;

@Slf4j
@RequiredArgsConstructor
public class AuraAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailResponseWriter responseWriter;

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) {
        log.warn("Forbidden request: path={}", request.getRequestURI());
        responseWriter.write(request, response, AuthExceptionFabric.createAccessDenied());
    }

}
