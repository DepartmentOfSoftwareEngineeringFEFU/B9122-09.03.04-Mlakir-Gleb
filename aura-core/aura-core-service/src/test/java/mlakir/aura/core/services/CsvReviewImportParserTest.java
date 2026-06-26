package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import mlakir.aura.core.exceptions.ManualImportExceptionFactory;
import org.junit.jupiter.api.Test;

class CsvReviewImportParserTest {

    private final CsvReviewImportParser parser = new CsvReviewImportParser(new ManualImportExceptionFactory());

    @Test
    void shouldParseValidRowsAndCountInvalidRows() {
        String csv = """
                externalId,text,authorName,publishedAt,originalUrl,rating
                1,"Good campus",Ivan,2026-04-01T12:00:00Z,,5
                2,"   ",Maria,2026-04-02T12:00:00Z,,4
                3,"Average",,invalid-date,,3
                4,"Works",Alex,2026-04-03T12:00:00Z,https://example.com,2
                """;

        CsvReviewImportParseResult result = parser.parse(new StringReader(csv));

        assertEquals(4, result.totalRows());
        assertEquals(2, result.invalidCount());
        assertEquals(2, result.rows().size());
        assertEquals("1", result.rows().getFirst().externalId());
        assertEquals("Works", result.rows().get(1).text());
    }

    @Test
    void shouldAllowMissingOptionalHeaders() {
        String csv = """
                externalId,text,publishedAt
                1,"Good campus",2026-04-01T12:00:00Z
                """;

        CsvReviewImportParseResult result = parser.parse(new StringReader(csv));

        assertEquals(1, result.totalRows());
        assertEquals(0, result.invalidCount());
        assertEquals(1, result.rows().size());
        assertEquals(null, result.rows().getFirst().authorName());
        assertEquals(null, result.rows().getFirst().originalUrl());
        assertEquals(null, result.rows().getFirst().rating());
    }
}
