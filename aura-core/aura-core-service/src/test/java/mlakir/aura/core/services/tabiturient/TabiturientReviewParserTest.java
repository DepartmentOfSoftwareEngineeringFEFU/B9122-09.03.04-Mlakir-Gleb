package mlakir.aura.core.services.tabiturient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import mlakir.aura.core.services.ReviewCandidate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TabiturientReviewParserTest {

    private TabiturientReviewParser parser;

    @BeforeEach
    void setUp() {
        TabiturientScraperProperties properties = new TabiturientScraperProperties();
        properties.setMaxReviewsPerRun(10);
        parser = new TabiturientReviewParser(properties);
    }

    @Test
    void shouldParseTabiturientReviewsFromFixture() throws Exception {
        List<ReviewCandidate> reviews = parser.parse(loadFixture(), "https://tabiturient.ru/vuzu/dvfu/");

        assertEquals(3, reviews.size());

        ReviewCandidate first = reviews.get(0);
        assertEquals("tabiturient:11568", first.externalId());
        assertEquals("Студент этого вуза", first.authorName());
        assertEquals(1, first.rating());
        assertEquals(OffsetDateTime.of(2026, 4, 11, 0, 0, 0, 0, ZoneOffset.UTC), first.publishedAt());
        assertEquals("https://tabiturient.ru/sliv/n/?11568", first.originalUrl());
        assertTrue(first.text().contains("скрытая часть текста"));
        assertFalse(first.text().contains("Показать полностью"));
        assertFalse(first.text().contains("..."));

        ReviewCandidate second = reviews.get(1);
        assertEquals("tabiturient:11569", second.externalId());
        assertEquals("Выпускник этого вуза", second.authorName());
        assertEquals(2, second.rating());
        assertEquals(OffsetDateTime.of(2026, 1, 30, 0, 0, 0, 0, ZoneOffset.UTC), second.publishedAt());
    }

    @Test
    void shouldFallbackToHashAndCurrentDateWhenIdOrDateMissing() throws Exception {
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1);

        List<ReviewCandidate> reviews = parser.parse(loadFixture(), "https://tabiturient.ru/vuzu/dvfu/");

        ReviewCandidate fallbackReview = reviews.get(2);
        OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(1);

        assertTrue(fallbackReview.externalId().startsWith("tabiturient:"));
        assertNotNull(fallbackReview.originalUrl());
        assertTrue(fallbackReview.publishedAt().isAfter(before));
        assertTrue(fallbackReview.publishedAt().isBefore(after));
    }

    @Test
    void shouldParseAjaxFragmentWithoutResultContainer() throws Exception {
        List<ReviewCandidate> reviews = parser.parse(loadAjaxFixture(), "https://tabiturient.ru/vuzu/dvfu/");

        assertEquals(2, reviews.size());
        assertEquals("tabiturient:11568", reviews.get(0).externalId());
        assertEquals("tabiturient:11569", reviews.get(1).externalId());
    }

    private Document loadFixture() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/tabiturient/tabiturient-dvfu.html")) {
            return Jsoup.parse(stream, "UTF-8", "https://tabiturient.ru/vuzu/dvfu/");
        }
    }

    private Document loadAjaxFixture() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/tabiturient/tabiturient-dvfu-ajax.html")) {
            return Jsoup.parse(stream, "UTF-8", "https://tabiturient.ru");
        }
    }
}
