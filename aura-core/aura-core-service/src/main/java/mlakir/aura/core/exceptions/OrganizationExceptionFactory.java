package mlakir.aura.core.exceptions;

import mlakir.aura.exception.AuraException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class OrganizationExceptionFactory extends AuraExceptionFactorySupport {

    public AuraException organizationNotFound(Long organizationId) {
        return build(HttpStatus.NOT_FOUND, "Organization not found",
                "Organization with id=" + organizationId + " was not found.", "organization_not_found");
    }

    public AuraException organizationAlreadyExists(String name) {
        return build(HttpStatus.CONFLICT, "Organization already exists",
                "Organization with name '" + name + "' already exists.", "organization_already_exists");
    }

    public AuraException organizationShortNameAlreadyExists(String shortName) {
        return build(HttpStatus.CONFLICT, "Organization short name already exists",
                "Organization with shortName '" + shortName + "' already exists.",
                "organization_short_name_already_exists");
    }

    public AuraException organizationInactive(Long organizationId) {
        return build(HttpStatus.BAD_REQUEST, "Organization inactive",
                "Organization with id=" + organizationId + " is inactive.", "organization_inactive");
    }

    public AuraException insufficientAnalyzedReviewsForInsights(Long organizationId) {
        return build(HttpStatus.BAD_REQUEST, "Not enough analyzed reviews for insights",
                "Недостаточно проанализированных отзывов для формирования отчёта. organizationId=" + organizationId,
                "organization_insights_not_enough_reviews");
    }

    public AuraException insightsGenerationUnavailable() {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Insights generation unavailable",
                "Organization insights could not be generated because analysis-service is unavailable.",
                "organization_insights_unavailable");
    }

    public AuraException invalidInsightsDate(String paramName, String value) {
        return build(HttpStatus.BAD_REQUEST, "Invalid insights date filter",
                "Query param '" + paramName + "' has invalid value '" + value + "'. Use ISO date or datetime.",
                "organization_insights_invalid_date");
    }

    public AuraException invalidInsightsDateRange() {
        return build(HttpStatus.BAD_REQUEST, "Invalid insights date range",
                "Query param 'from' must be earlier than or equal to 'to'.",
                "organization_insights_invalid_range");
    }
}
