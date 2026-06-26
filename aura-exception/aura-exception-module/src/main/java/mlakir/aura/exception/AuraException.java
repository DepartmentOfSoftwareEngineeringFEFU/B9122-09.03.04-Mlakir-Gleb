package mlakir.aura.exception;

import lombok.*;
import org.springframework.http.*;
import org.springframework.web.*;

@Getter
public class AuraException extends ErrorResponseException {

    public AuraException(HttpStatus statusCode, ProblemDetail problemDetail) {
        super(statusCode, problemDetail, null);
    }

}
