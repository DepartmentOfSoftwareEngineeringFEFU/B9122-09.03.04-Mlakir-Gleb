package mlakir.aura.core.services.tabiturient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.enums.CollectionMode;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.exceptions.SourceExceptionFactory;
import mlakir.aura.core.models.OrganizationEntity;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.services.ReviewCandidate;
import mlakir.aura.exception.AuraException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TabiturientSourceCollectorTest {

    @Mock
    private SourceBaseUrlNormalizer sourceBaseUrlNormalizer;
    @Mock
    private HtmlPageFetcher htmlPageFetcher;
    @Mock
    private TabiturientAjaxClient tabiturientAjaxClient;
    @Mock
    private TabiturientReviewParser parser;

    private TabiturientScraperProperties properties;
    private TabiturientSourceCollector collector;

    @BeforeEach
    void setUp() {
        properties = new TabiturientScraperProperties();
        properties.setPageSize(25);
        properties.setMaxReviewsPerRun(100);
        properties.setRequestDelayMs(0);
        collector = new TabiturientSourceCollector(
                sourceBaseUrlNormalizer,
                htmlPageFetcher,
                tabiturientAjaxClient,
                parser,
                properties
        );
    }

    @Test
    void shouldSupportOnlyTabiturient() {
        assertTrue(collector.supports(SourceType.TABITURIENT));
        assertFalse(collector.supports(SourceType.MANUAL_IMPORT));
    }

    @Test
    void shouldFetchAndParseNormalizedUrl() {
        SourceEntity source = source();
        Document document = Jsoup.parse("<html></html>");
        ReviewCandidate review = new ReviewCandidate(
                "tabiturient:11568",
                "Очень полезный отзыв о вузе",
                "Студент этого вуза",
                1,
                OffsetDateTime.now(),
                "https://tabiturient.ru/sliv/n/?11568"
        );

        when(sourceBaseUrlNormalizer.normalizeTabiturientUrl(source.getBaseUrl()))
                .thenReturn("https://tabiturient.ru/vuzu/dvfu/");
        when(sourceBaseUrlNormalizer.extractTabiturientVuzId("https://tabiturient.ru/vuzu/dvfu/"))
                .thenReturn("dvfu");
        when(htmlPageFetcher.fetch("https://tabiturient.ru/vuzu/dvfu/")).thenReturn(document);
        when(parser.parse(document, "https://tabiturient.ru/vuzu/dvfu/")).thenReturn(List.of(review));
        when(tabiturientAjaxClient.fetchReviews("dvfu", 50)).thenThrow(new TabiturientScrapingException("stop"));

        List<ReviewCandidate> result = collector.collect(source);

        assertEquals(1, result.size());
        verify(htmlPageFetcher).fetch("https://tabiturient.ru/vuzu/dvfu/");
        verify(parser).parse(document, "https://tabiturient.ru/vuzu/dvfu/");
    }

    @Test
    void shouldCollectMoreThanTwentyFiveReviewsUsingAjaxPagination() {
        SourceEntity source = source();
        Document initialDocument = Jsoup.parse("<html></html>");
        Document ajax50Document = Jsoup.parse("<html></html>");
        Document ajax75Document = Jsoup.parse("<html></html>");

        when(sourceBaseUrlNormalizer.normalizeTabiturientUrl(source.getBaseUrl()))
                .thenReturn("https://tabiturient.ru/vuzu/dvfu/");
        when(sourceBaseUrlNormalizer.extractTabiturientVuzId("https://tabiturient.ru/vuzu/dvfu/"))
                .thenReturn("dvfu");
        when(htmlPageFetcher.fetch("https://tabiturient.ru/vuzu/dvfu/")).thenReturn(initialDocument);
        when(tabiturientAjaxClient.fetchReviews("dvfu", 50)).thenReturn(ajax50Document);
        when(tabiturientAjaxClient.fetchReviews("dvfu", 75)).thenReturn(ajax75Document);

        when(parser.parse(initialDocument, "https://tabiturient.ru/vuzu/dvfu/"))
                .thenReturn(reviews(1, 25));
        when(parser.parse(ajax50Document, "https://tabiturient.ru/vuzu/dvfu/"))
                .thenReturn(reviews(1, 50));
        when(parser.parse(ajax75Document, "https://tabiturient.ru/vuzu/dvfu/"))
                .thenReturn(reviews(1, 50));

        List<ReviewCandidate> result = collector.collect(source);

        assertEquals(50, result.size());
        verify(tabiturientAjaxClient).fetchReviews("dvfu", 50);
        verify(tabiturientAjaxClient).fetchReviews("dvfu", 75);
    }

    @Test
    void shouldStopWhenAjaxReturnsNoNewExternalIds() {
        SourceEntity source = source();
        Document initialDocument = Jsoup.parse("<html></html>");
        Document ajax50Document = Jsoup.parse("<html></html>");

        when(sourceBaseUrlNormalizer.normalizeTabiturientUrl(source.getBaseUrl()))
                .thenReturn("https://tabiturient.ru/vuzu/dvfu/");
        when(sourceBaseUrlNormalizer.extractTabiturientVuzId("https://tabiturient.ru/vuzu/dvfu/"))
                .thenReturn("dvfu");
        when(htmlPageFetcher.fetch("https://tabiturient.ru/vuzu/dvfu/")).thenReturn(initialDocument);
        when(tabiturientAjaxClient.fetchReviews("dvfu", 50)).thenReturn(ajax50Document);
        when(parser.parse(initialDocument, "https://tabiturient.ru/vuzu/dvfu/"))
                .thenReturn(reviews(1, 25));
        when(parser.parse(ajax50Document, "https://tabiturient.ru/vuzu/dvfu/"))
                .thenReturn(reviews(1, 25));

        List<ReviewCandidate> result = collector.collect(source);

        assertEquals(25, result.size());
        verify(tabiturientAjaxClient).fetchReviews("dvfu", 50);
    }

    @Test
    void shouldRejectInvalidUrl() {
        SourceEntity source = source();
        AuraException exception = new SourceExceptionFactory().invalidTabiturientUrl(source.getBaseUrl());

        when(sourceBaseUrlNormalizer.normalizeTabiturientUrl(source.getBaseUrl())).thenThrow(exception);

        assertThrows(AuraException.class, () -> collector.collect(source));
    }

    private SourceEntity source() {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(1L);
        organization.setName("DVFU");
        organization.setShortName("DVFU");
        organization.setIsActive(true);

        SourceEntity source = new SourceEntity();
        source.setId(1L);
        source.setOrganization(organization);
        source.setName("Отзывы Tabiturient о ДВФУ");
        source.setType(SourceType.TABITURIENT);
        source.setCollectionMode(CollectionMode.MANUAL);
        source.setBaseUrl("https://tabiturient.ru/vuzu/dvfu/");
        source.setIsActive(true);
        return source;
    }

    private List<ReviewCandidate> reviews(int fromInclusive, int toInclusive) {
        return java.util.stream.IntStream.rangeClosed(fromInclusive, toInclusive)
                .mapToObj(index -> new ReviewCandidate(
                        "tabiturient:" + index,
                        "Очень полезный отзыв номер " + index,
                        "Студент этого вуза",
                        1,
                        OffsetDateTime.now(),
                        "https://tabiturient.ru/sliv/n/?" + index
                ))
                .toList();
    }
}
