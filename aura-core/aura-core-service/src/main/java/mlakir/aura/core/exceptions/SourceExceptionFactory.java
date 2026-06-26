package mlakir.aura.core.exceptions;

import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.exceptions.AuraExceptionFactorySupport;
import mlakir.aura.exception.AuraException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SourceExceptionFactory extends AuraExceptionFactorySupport {

    public AuraException sourceNotFound(Long sourceId) {
        return build(HttpStatus.NOT_FOUND, "Source not found",
                "Source with id=" + sourceId + " was not found.", "source_not_found");
    }

    public AuraException duplicateSource(String name, SourceType type) {
        return build(HttpStatus.CONFLICT, "Duplicate source",
                "Source with name '" + name + "' and type '" + type + "' already exists.",
                "duplicate_source");
    }

    public AuraException sourceInactive(Long sourceId) {
        return build(HttpStatus.BAD_REQUEST, "Source inactive",
                "Source with id=" + sourceId + " is inactive and cannot be used for import or collection.",
                "source_inactive");
    }

    public AuraException unsupportedSourceType(SourceType sourceType) {
        return build(HttpStatus.BAD_REQUEST, "Unsupported source type",
                "Source type '" + sourceType + "' is not supported for this operation.",
                "unsupported_source_type");
    }

    public AuraException invalidTabiturientUrl(String baseUrl) {
        return build(HttpStatus.BAD_REQUEST, "Invalid Tabiturient URL",
                "Source baseUrl '" + safe(baseUrl) + "' must match https://tabiturient.ru/vuzu/{slug}/",
                "invalid_tabiturient_url");
    }

    public AuraException invalidOtzovikUrl(String baseUrl) {
        return build(HttpStatus.BAD_REQUEST, "Invalid Otzovik URL",
                "Source baseUrl '" + safe(baseUrl) + "' must be an otzovik.com /reviews/ URL.",
                "invalid_otzovik_url");
    }

    public AuraException invalidVuzopediaUrl(String baseUrl) {
        return build(HttpStatus.BAD_REQUEST, "Invalid Vuzopedia URL",
                "Source baseUrl '" + safe(baseUrl) + "' must match https://vuzopedia.ru/vuz/{id}/otziv.",
                "invalid_vuzopedia_url");
    }

    public AuraException invalidScheduleInterval() {
        return build(HttpStatus.BAD_REQUEST, "Invalid schedule interval",
                "scheduleIntervalMinutes is required when scheduleEnabled=true and must be between 15 and 43200.",
                "invalid_schedule_interval");
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
