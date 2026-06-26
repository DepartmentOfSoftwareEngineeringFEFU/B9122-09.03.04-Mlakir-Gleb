package mlakir.aura.auth.handler;

import java.io.*;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.*;
import lombok.*;
import mlakir.aura.exception.*;
import org.springframework.http.*;

@RequiredArgsConstructor
public class ProblemDetailResponseWriter {

    private final ObjectMapper objectMapper;

    private final AuraExceptionHandler auraExceptionHandler;

    public void write(
        HttpServletRequest request,
        HttpServletResponse response,
        AuraException exception
    ) {
        try {
            ProblemDetail problemDetail = auraExceptionHandler.handleAuraException(exception, request);

            response.setStatus(problemDetail.getStatus());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            objectMapper.writeValue(response.getOutputStream(), problemDetail);
        } catch (IOException ex) {
            response.resetBuffer();
            response.setStatus(HttpStatus.valueOf(exception.getStatusCode().value()).value());
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        }
    }

}
