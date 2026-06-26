package mlakir.aura.core.services.vuzopedia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.exceptions.SourceExceptionFactory;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.services.ReviewCandidate;
import mlakir.aura.core.services.tabiturient.SourceBaseUrlNormalizer;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class VuzopediaSourceCollectorTest {

    private VuzopediaHtmlPageFetcher htmlPageFetcher;
    private VuzopediaSourceCollector collector;

    @BeforeEach
    void setUp() {
        htmlPageFetcher = Mockito.mock(VuzopediaHtmlPageFetcher.class);
        VuzopediaScraperProperties properties = new VuzopediaScraperProperties();
        properties.setMaxReviewsPerRun(10);
        properties.setRequestDelayMs(0);
        collector = new VuzopediaSourceCollector(
                new SourceBaseUrlNormalizer(new SourceExceptionFactory()),
                htmlPageFetcher,
                new VuzopediaReviewParser(),
                properties
        );
    }

    @Test
    void shouldCollectVuzopediaReviews() {
        SourceEntity source = new SourceEntity();
        source.setId(1L);
        source.setType(SourceType.VUZOPEDIA);
        source.setBaseUrl("https://vuzopedia.ru/vuz/3281/otziv");

        when(htmlPageFetcher.fetch("https://vuzopedia.ru/vuz/3281/otziv")).thenReturn(Jsoup.parse("""
                <div class="otzivItem" style="border-left: 4px solid #607d8b">
                  Нейтральный отзыв.
                  <div class="otzivInfo">2025-08-30, от student</div>
                </div>
                """, "https://vuzopedia.ru/vuz/3281/otziv"));

        List<ReviewCandidate> reviews = collector.collect(source);

        assertEquals(1, reviews.size());
        assertEquals("student", reviews.getFirst().authorName());
        org.junit.jupiter.api.Assertions.assertTrue(reviews.getFirst().externalId().startsWith("vuzopedia:3281:"));
    }
}
