package mlakir.aura.core.services.vuzopedia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class VuzopediaReviewParserTest {

    private final VuzopediaReviewParser parser = new VuzopediaReviewParser();

    @Test
    void shouldParseVuzopediaReviewCards() {
        List<VuzopediaReviewData> reviews = parser.parse(fixture(), "3281");

        assertEquals(2, reviews.size());

        VuzopediaReviewData first = reviews.getFirst();
        assertTrue(first.externalId().startsWith("vuzopedia:3281:2025-08-30:"));
        assertEquals(OffsetDateTime.of(2025, 8, 30, 0, 0, 0, 0, ZoneOffset.UTC), first.publishedAt());
        assertEquals("poshukai", first.authorName());
        assertEquals(VuzopediaSentimentHint.POSITIVE, first.sentimentHint());
        assertTrue(first.text().contains("Хороший университет"));
        assertFalse(first.text().contains("Отзыв является личным мнением автора"));
        assertFalse(first.text().contains("Ответ представителя"));

        VuzopediaReviewData second = reviews.get(1);
        assertEquals("аноним", second.authorName());
        assertEquals(VuzopediaSentimentHint.NEGATIVE, second.sentimentHint());
    }

    @Test
    void shouldGenerateStableExternalId() {
        List<VuzopediaReviewData> firstParse = parser.parse(fixture(), "3281");
        List<VuzopediaReviewData> secondParse = parser.parse(fixture(), "3281");

        assertEquals(firstParse.getFirst().externalId(), secondParse.getFirst().externalId());
    }

    private Document fixture() {
        String html = """
                <html><body>
                  <div class="otzivItem" style="border-left: 4px solid #4CAF50">
                    Хороший университет с сильными преподавателями.
                    <div class="otzivInfo">2025-08-30, от poshukai</div>
                    <div class="otzPredAns">Ответ представителя вуза</div>
                    Отзыв является личным мнением автора и может не соответствовать действительности
                  </div>
                  <div class="otzivItem" style="border-left: 4px solid #F44336">
                    Есть проблемы с общежитием.
                    <div class="otzivInfo">2025-08-31, от анонима</div>
                  </div>
                  <div class="otzivItem" style="border-left: 4px solid #607d8b">
                    <div class="otzivInfo">2025-09-01, от empty</div>
                  </div>
                </body></html>
                """;
        return Jsoup.parse(html, "https://vuzopedia.ru/vuz/3281/otziv");
    }
}
