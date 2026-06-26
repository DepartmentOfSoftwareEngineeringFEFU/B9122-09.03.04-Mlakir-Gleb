package mlakir.aura.core.exceptions;

import mlakir.aura.exception.AuraException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ManualImportExceptionFactory extends AuraExceptionFactorySupport {

    public AuraException fileMissing() {
        return build(HttpStatus.BAD_REQUEST, "Import file is missing",
                "CSV file must be provided in multipart field 'file'.", "manual_import_file_missing");
    }

    public AuraException emptyFile() {
        return build(HttpStatus.BAD_REQUEST, "Import file is empty",
                "CSV file is empty.", "manual_import_empty_file");
    }

    public AuraException invalidFileType(String fileName) {
        return build(HttpStatus.BAD_REQUEST, "Invalid import file type",
                "Expected CSV file, got '" + safe(fileName) + "'.", "manual_import_invalid_file_type");
    }

    public AuraException invalidCsvFormat(String message) {
        return build(HttpStatus.BAD_REQUEST, "Invalid CSV format",
                safe(message), "manual_import_invalid_csv");
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
