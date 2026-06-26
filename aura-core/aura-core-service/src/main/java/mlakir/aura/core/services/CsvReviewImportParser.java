package mlakir.aura.core.services;

import java.io.IOException;
import java.io.Reader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.exceptions.ManualImportExceptionFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CsvReviewImportParser {

    private static final String HEADER_EXTERNAL_ID = "externalId";
    private static final String HEADER_TEXT = "text";
    private static final String HEADER_AUTHOR_NAME = "authorName";
    private static final String HEADER_PUBLISHED_AT = "publishedAt";
    private static final String HEADER_ORIGINAL_URL = "originalUrl";
    private static final String HEADER_RATING = "rating";

    private final ManualImportExceptionFactory manualImportExceptionFactory;

    public CsvReviewImportParseResult parse(Reader reader) {
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get()
                .parse(reader)) {
            int totalRows = 0;
            int invalidCount = 0;
            List<ParsedCsvReviewRow> rows = new ArrayList<>();

            for (CSVRecord record : parser) {
                totalRows++;
                ParsedCsvReviewRow row = toRow(record);
                if (row == null) {
                    invalidCount++;
                    continue;
                }
                rows.add(row);
            }

            return new CsvReviewImportParseResult(totalRows, invalidCount, rows);
        } catch (IllegalArgumentException | IOException exception) {
            throw manualImportExceptionFactory.invalidCsvFormat("Failed to parse CSV file: " + exception.getMessage());
        }
    }

    private ParsedCsvReviewRow toRow(CSVRecord record) {
        String externalId = normalize(getValue(record, HEADER_EXTERNAL_ID));
        String text = normalize(getValue(record, HEADER_TEXT));
        String publishedAtRaw = normalize(getValue(record, HEADER_PUBLISHED_AT));

        if (externalId == null || text == null || publishedAtRaw == null) {
            return null;
        }

        OffsetDateTime publishedAt;
        try {
            publishedAt = OffsetDateTime.parse(publishedAtRaw);
        } catch (DateTimeParseException exception) {
            return null;
        }

        String ratingRaw = normalize(getValue(record, HEADER_RATING));
        Integer rating = parseRating(ratingRaw);
        if (ratingRaw != null && rating == null) {
            return null;
        }

        return new ParsedCsvReviewRow(
                externalId,
                text,
                normalize(getValue(record, HEADER_AUTHOR_NAME)),
                publishedAt,
                normalize(getValue(record, HEADER_ORIGINAL_URL)),
                rating
        );
    }

    private String getValue(CSVRecord record, String headerName) {
        if (!record.isMapped(headerName) || !record.isSet(headerName)) {
            return null;
        }
        return record.get(headerName);
    }

    private Integer parseRating(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
