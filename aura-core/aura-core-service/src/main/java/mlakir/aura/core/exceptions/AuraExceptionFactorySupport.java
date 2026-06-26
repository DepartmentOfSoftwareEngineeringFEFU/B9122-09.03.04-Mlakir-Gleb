package mlakir.aura.core.exceptions;

import mlakir.aura.exception.AuraException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public abstract class AuraExceptionFactorySupport {

    protected AuraException build(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("code", code);
        return new AuraException(status, problemDetail);
    }
}
