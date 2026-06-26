package mlakir.aura.exception;

import java.net.*;
import java.time.*;
import java.util.*;

import jakarta.servlet.http.*;
import jakarta.validation.*;
import lombok.*;
import lombok.NonNull;
import lombok.extern.slf4j.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.lang.*;
import org.springframework.web.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.web.method.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.*;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class AuraExceptionHandler extends ResponseEntityExceptionHandler {

    private final AuraProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(AuraException.class)
    public ProblemDetail handleAuraException(AuraException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = enrich(ex.getBody(), request);
        logClientError("AuraException", problemDetail, request, ex.getMessage());
        return problemDetail;
    }

    @Override
    protected @NonNull ResponseEntity<Object> handleErrorResponseException(
        @NonNull ErrorResponseException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        if (ex instanceof AuraException auraException) {
            ProblemDetail problemDetail = withRequest(auraException.getBody(), request);
            logClientError("AuraException", problemDetail, request, ex.getMessage());
            return ResponseEntity
                .status(status)
                .headers(headers)
                .body(problemDetail);
        }

        ProblemDetail problemDetail = withRequest(ex.getBody(), request);
        logClientError(ex.getClass().getSimpleName(), problemDetail, request, ex.getMessage());
        return ResponseEntity
            .status(status)
            .headers(headers)
            .body(problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problemDetail = enrich(problemDetailFactory.buildIllegalArgumentError(ex), request);
        logClientError("IllegalArgumentException", problemDetail, request, ex.getMessage());
        return ResponseEntity.badRequest()
            .body(problemDetail);
    }

    @Override
    protected @NonNull ResponseEntity<Object> handleMethodArgumentNotValid(
        @NonNull MethodArgumentNotValidException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        ProblemDetail problemDetail = withRequest(problemDetailFactory.buildValidationError(ex), request);
        logClientError("MethodArgumentNotValidException", problemDetail, request, ex.getMessage());
        return ResponseEntity.status(status).body(problemDetail);
    }

    @Nullable
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
        @NonNull HandlerMethodValidationException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        ProblemDetail problemDetail = problemDetailFactory.buildMethodValidationError(ex, status);
        logClientError("HandlerMethodValidationException", problemDetail, request, ex.getMessage());

        return handleExceptionInternal(
            ex,
            withRequest(problemDetail, request),
            headers,
            status,
            request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problemDetail = problemDetailFactory.buildConstraintViolationError(ex);
        logClientError("ConstraintViolationException", problemDetail, request, ex.getMessage());
        return ResponseEntity.badRequest().body(enrich(problemDetail, request));
    }

    @Nullable
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
        @NonNull org.springframework.beans.TypeMismatchException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        ProblemDetail problemDetail = problemDetailFactory.buildTypeMismatchError(ex, status);
        logClientError(ex.getClass().getSimpleName(), problemDetail, request, ex.getMessage());

        return handleExceptionInternal(
            ex,
            withRequest(problemDetail, request),
            headers,
            status,
            request);
    }

    @Nullable
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        @NonNull HttpMessageNotReadableException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        ProblemDetail problemDetail = problemDetailFactory.buildMalformedRequestError(status);
        logClientError("HttpMessageNotReadableException", problemDetail, request, ex.getMessage());

        return handleExceptionInternal(
            ex,
            withRequest(problemDetail, request),
            headers,
            status,
            request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingRequestHeader(
        MissingRequestHeaderException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problemDetail = enrich(problemDetailFactory.buildMissingRequestHeaderError(ex), request);
        logClientError("MissingRequestHeaderException", problemDetail, request, ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
        Exception ex,
        HttpServletRequest request
    ) {

        String errorId = UUID.randomUUID().toString();
        ProblemDetail problemDetail = problemDetailFactory.buildUnexpectedError(ex);
        problemDetail.setProperty("errorId", errorId);
        ProblemDetail enrichedProblemDetail = enrich(problemDetail, request);
        log.error(
            "Unhandled exception [{}] {} {} -> status={} detail={}",
            errorId,
            requestMethod(request),
            requestPath(request),
            enrichedProblemDetail.getStatus(),
            enrichedProblemDetail.getDetail(),
            ex
        );

        return ResponseEntity
            .status(enrichedProblemDetail.getStatus())
            .body(enrichedProblemDetail);
    }

    private ProblemDetail enrich(ProblemDetail problemDetail, HttpServletRequest request) {
        if (problemDetail.getInstance() == null && request != null) {
            problemDetail.setInstance(URI.create(request.getRequestURI()));
        }
        if (problemDetail.getProperties() == null || !problemDetail.getProperties()
            .containsKey("timestamp")) {
            problemDetail.setProperty("timestamp", Instant.now());
        }
        return problemDetail;
    }

    private ProblemDetail withRequest(ProblemDetail problemDetail, WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return enrich(problemDetail, servletWebRequest.getRequest());
        }
        return problemDetail;
    }

    private void logClientError(
        String source,
        ProblemDetail problemDetail,
        WebRequest request,
        String message
    ) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            logClientError(source, problemDetail, servletWebRequest.getRequest(), message);
        }
    }

    private void logClientError(
        String source,
        ProblemDetail problemDetail,
        HttpServletRequest request,
        String message
    ) {
        log.warn(
            "{} {} {} -> status={} detail={} message={}",
            source,
            requestMethod(request),
            requestPath(request),
            problemDetail.getStatus(),
            problemDetail.getDetail(),
            message
        );
    }

    private String requestMethod(HttpServletRequest request) {
        return request != null ? request.getMethod() : "N/A";
    }

    private String requestPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "N/A";
    }

}
