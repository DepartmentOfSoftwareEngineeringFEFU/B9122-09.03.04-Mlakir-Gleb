package mlakir.aura.core.services;

import java.util.List;

public record CsvReviewImportParseResult(
        int totalRows,
        int invalidCount,
        List<ParsedCsvReviewRow> rows
) {
}
