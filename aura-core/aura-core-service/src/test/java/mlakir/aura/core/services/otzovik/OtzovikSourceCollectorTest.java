package mlakir.aura.core.services.otzovik;

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

class OtzovikSourceCollectorTest {

    private OtzovikHtmlPageFetcher htmlPageFetcher;
    private OtzovikSourceCollector collector;

    @BeforeEach
    void setUp() {
        htmlPageFetcher = Mockito.mock(OtzovikHtmlPageFetcher.class);
        OtzovikScraperProperties properties = new OtzovikScraperProperties();
        properties.setMaxReviewsPerRun(10);
        properties.setRequestDelayMs(0);
        collector = new OtzovikSourceCollector(
                new SourceBaseUrlNormalizer(new SourceExceptionFactory()),
                htmlPageFetcher,
                new OtzovikReviewParser(),
                properties
        );
    }

    @Test
    void shouldUseTeaserFallbackWhenFullReviewPageFails() {
        SourceEntity source = new SourceEntity();
        source.setId(1L);
        source.setType(SourceType.OTZOVIK);
        source.setBaseUrl("https://otzovik.com/reviews/dvfu/");

        when(htmlPageFetcher.fetch("https://otzovik.com/reviews/dvfu/")).thenReturn(Jsoup.parse("""
                <div itemprop="review">
                  <meta itemprop="url" content="/review_6966870.html">
                  <a class="review-title" itemprop="name" href="/review_6966870.html">Заголовок</a>
                  <div class="review-teaser" itemprop="description">Тизер отзыва</div>
                </div>
                """, "https://otzovik.com/reviews/dvfu/"));
        when(htmlPageFetcher.fetch("https://otzovik.com/review_6966870.html"))
                .thenThrow(new OtzovikScrapingException("HTTP 403"));

        List<ReviewCandidate> reviews = collector.collect(source);

        assertEquals(1, reviews.size());
        assertEquals("otzovik:review_6966870", reviews.getFirst().externalId());
        org.junit.jupiter.api.Assertions.assertTrue(reviews.getFirst().text().contains("Тизер отзыва"));
    }
}
