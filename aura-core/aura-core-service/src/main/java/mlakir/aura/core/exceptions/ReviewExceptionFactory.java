package mlakir.aura.core.exceptions;

import mlakir.aura.exception.AuraException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ReviewExceptionFactory extends AuraExceptionFactorySupport {

    public AuraException reviewNotFound(Long reviewId) {
        return build(HttpStatus.NOT_FOUND, "Review not found",
                "Review with id=" + reviewId + " was not found.", "review_not_found");
    }

    public AuraException summaryGenerationUnavailable() {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Summary generation unavailable",
                "Summary could not be generated because analysis-service is unavailable.",
                "review_summary_unavailable");
    }
}
