package mlakir.aura.core.services.otzovik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.services.ReviewCandidate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class OtzovikReviewParserTest {

    private final OtzovikReviewParser parser = new OtzovikReviewParser();

    @Test
    void shouldParseReviewCardsFromListPage() {
        List<OtzovikReviewCard> cards = parser.parseListPage(listDocument());

        assertEquals(1, cards.size());
        OtzovikReviewCard card = cards.getFirst();
        assertEquals("review_6966870", card.externalId());
        assertEquals("https://otzovik.com/review_6966870.html", card.reviewUrl());
        assertEquals("ДВФУ - хороший университет", card.title());
        assertEquals("Краткий текст отзыва", card.teaser());
        assertEquals(4, card.rating());
        assertEquals(OffsetDateTime.parse("2024-02-18T12:45:04+03:00"), card.publishedAt());
        assertEquals("Иван", card.authorName());
        assertEquals("кампус, преподаватели", card.pros());
        assertEquals("дорого", card.cons());
        assertEquals(12, card.likesCount());
        assertEquals(3, card.commentsCount());
    }

    @Test
    void shouldParseFullReviewAndBuildCandidateText() {
        OtzovikReviewCard card = parser.parseListPage(listDocument()).getFirst();
        OtzovikFullReview fullReview = parser.parseFullReview(fullDocument());

        ReviewCandidate candidate = parser.toCandidate(card, fullReview);

        assertEquals("otzovik:review_6966870", candidate.externalId());
        assertEquals("https://otzovik.com/review_6966870.html", candidate.originalUrl());
        assertEquals("Петр", candidate.authorName());
        assertEquals(5, candidate.rating());
        assertTrue(candidate.text().contains("Полный текст отзыва"));
        assertTrue(candidate.text().contains("Достоинства: сильные преподаватели"));
        assertTrue(candidate.text().contains("Недостатки: мало парковок"));
    }

    @Test
    void shouldUseTeaserFallbackWhenFullTextIsMissing() {
        OtzovikReviewCard card = parser.parseListPage(listDocument()).getFirst();

        ReviewCandidate candidate = parser.toFallbackCandidate(card);

        assertTrue(candidate.text().contains("Краткий текст отзыва"));
        assertTrue(candidate.text().contains("Достоинства: кампус, преподаватели"));
        assertTrue(candidate.text().contains("Недостатки: дорого"));
    }

    private Document listDocument() {
        String html = """
                <html><body>
                  <div itemprop="review">
                    <meta itemprop="url" content="/review_6966870.html">
                    <a class="review-title" itemprop="name" href="/review_6966870.html">ДВФУ - хороший университет</a>
                    <div class="review-teaser" itemprop="description">Краткий текст отзыва</div>
                    <meta itemprop="ratingValue" content="4">
                    <div class="review-postdate" itemprop="datePublished" content="2024-02-18T12:45:04+03:00"></div>
                    <div class="item-left"><span itemprop="name">Иван</span></div>
                    <div class="review-plus">Достоинства: кампус, преподаватели</div>
                    <div class="review-minus">Недостатки: дорого</div>
                    <a class="review-yes"><span>12</span></a>
                    <span itemprop="commentCount">3</span>
                  </div>
                </body></html>
                """;
        return Jsoup.parse(html, "https://otzovik.com/reviews/test/");
    }

    private Document fullDocument() {
        String html = """
                <html><body>
                  <h1 itemprop="name">Полный заголовок</h1>
                  <div itemprop="reviewBody">Полный текст отзыва про обучение и кампус.</div>
                  <meta itemprop="ratingValue" content="5">
                  <div class="review-postdate" itemprop="datePublished" content="2024-03-01T10:00:00+03:00"></div>
                  <div class="item-left"><span itemprop="name">Петр</span></div>
                  <div class="review-plus">Достоинства: сильные преподаватели</div>
                  <div class="review-minus">Недостатки: мало парковок</div>
                </body></html>
                """;
        return Jsoup.parse(html, "https://otzovik.com/review_6966870.html");
    }
}
