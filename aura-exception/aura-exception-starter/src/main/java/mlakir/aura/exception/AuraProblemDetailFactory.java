package mlakir.aura.exception;

import java.util.*;

import jakarta.validation.*;
import org.springframework.beans.*;
import org.springframework.context.*;
import org.springframework.core.*;
import org.springframework.http.*;
import org.springframework.validation.method.*;
import org.springframework.web.bind.*;
import org.springframework.web.method.annotation.*;

public class AuraProblemDetailFactory {

    private final AuraExceptionProperties properties;

    public AuraProblemDetailFactory(AuraExceptionProperties properties) {
        this.properties = properties;
    }

    public ProblemDetail buildValidationError(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            ex.getStatusCode(),
            properties.getValidationErrorDetail()
        );
        problemDetail.setType(properties.getValidationErrorType());
        problemDetail.setTitle(properties.getValidationErrorTitle());
        problemDetail.setInstance(ex.getBody().getInstance());
        problemDetail.setProperty("errors", getValidationErrors(ex));
        return problemDetail;
    }

    public ProblemDetail buildMethodValidationError(
        HandlerMethodValidationException ex,
        HttpStatusCode status
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            status,
            properties.getConstraintViolationDetail()
        );
        problemDetail.setType(properties.getValidationErrorType());
        problemDetail.setTitle(properties.getValidationErrorTitle());
        problemDetail.setProperty("errors", getMethodValidationErrors(ex));
        return problemDetail;
    }

    public ProblemDetail buildConstraintViolationError(ConstraintViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            properties.getConstraintViolationDetail()
        );
        problemDetail.setType(properties.getValidationErrorType());
        problemDetail.setTitle(properties.getValidationErrorTitle());
        problemDetail.setProperty("errors", getConstraintViolationErrors(ex));
        return problemDetail;
    }

    public ProblemDetail buildIllegalArgumentError(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            properties.getIllegalArgumentDetail()
        );
        problemDetail.setTitle(properties.getIllegalArgumentTitle());
        problemDetail.setType(properties.getDefaultType());
        problemDetail.setProperty("message", ex.getMessage());
        return problemDetail;
    }

    public ProblemDetail buildTypeMismatchError(
        TypeMismatchException ex,
        HttpStatusCode status
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            status,
            properties.getTypeMismatchDetail()
        );
        problemDetail.setTitle(properties.getTypeMismatchTitle());
        if (ex instanceof MethodArgumentTypeMismatchException mismatchException) {
            problemDetail.setProperty(
                "errors",
                Map.of(getParameterName(mismatchException), buildTypeMismatchMessage(mismatchException))
            );
        }
        return problemDetail;
    }

    public ProblemDetail buildMalformedRequestError(HttpStatusCode status) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            status,
            properties.getMalformedRequestDetail()
        );
        problemDetail.setTitle(properties.getMalformedRequestTitle());
        return problemDetail;
    }

    public ProblemDetail buildMissingRequestHeaderError(MissingRequestHeaderException ex) {
        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(ex.getHeaderName())) {
            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
            problemDetail.setTitle(properties.getMissingTokenTitle());
            problemDetail.setDetail(properties.getMissingTokenDetail());
            return problemDetail;
        }

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle(properties.getMissingRequestHeaderTitle());
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    public ProblemDetail buildUnexpectedError(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(
            HttpStatusCode.valueOf(properties.getDefaultStatus())
        );
        problemDetail.setType(properties.getDefaultType());
        problemDetail.setTitle(properties.getDefaultTitle());
        problemDetail.setDetail(properties.getDefaultDetail());
        if (properties.isIncludeExceptionMessage() && ex.getMessage() != null) {
            problemDetail.setProperty("message", ex.getMessage());
        }
        return problemDetail;
    }

    private Map<String, String> getValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String message = Objects.toString(
                error.getDefaultMessage(),
                properties.getValidationErrorDefaultFieldMessage()
            );
            errors.put(error.getField(), message);
        });

        return errors;
    }

    private Map<String, String> getMethodValidationErrors(HandlerMethodValidationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getAllValidationResults().forEach(result -> appendMethodValidationErrors(errors, result));

        return errors;
    }

    private void appendMethodValidationErrors(
        Map<String, String> errors,
        ParameterValidationResult result
    ) {
        if (result instanceof ParameterErrors parameterErrors && parameterErrors.hasFieldErrors()) {
            parameterErrors.getFieldErrors().forEach(error -> errors.put(
                error.getField(),
                Objects.toString(error.getDefaultMessage(), properties.getValidationErrorDefaultFieldMessage())
            ));
            return;
        }

        String parameterName = getParameterName(result.getMethodParameter());
        String message = result.getResolvableErrors().stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(properties.getValidationErrorDefaultFieldMessage());

        errors.put(parameterName, message);
    }

    private Map<String, String> getConstraintViolationErrors(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(violation -> errors.put(
            getConstraintViolationPath(violation),
            Objects.toString(violation.getMessage(), properties.getValidationErrorDefaultFieldMessage())
        ));

        return errors;
    }

    private String getConstraintViolationPath(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < path.length() - 1) {
            return path.substring(dotIndex + 1);
        }
        return path;
    }

    private String buildTypeMismatchMessage(MethodArgumentTypeMismatchException ex) {
        Class<?> requiredType = ex.getRequiredType();
        if (requiredType == null) {
            return properties.getValidationErrorDefaultFieldMessage();
        }

        return "Expected type: " + requiredType.getSimpleName();
    }

    private String getParameterName(MethodArgumentTypeMismatchException ex) {
        return Objects.toString(ex.getName(), "parameter");
    }

    private String getParameterName(MethodParameter parameter) {
        String parameterName = parameter.getParameterName();
        if (parameterName != null) {
            return parameterName;
        }
        return "arg" + parameter.getParameterIndex();
    }

}
