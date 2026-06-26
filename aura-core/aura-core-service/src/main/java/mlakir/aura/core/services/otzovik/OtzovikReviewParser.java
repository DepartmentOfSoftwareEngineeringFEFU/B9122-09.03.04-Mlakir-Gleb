package mlakir.aura.core.services.otzovik;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.services.ReviewCandidate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OtzovikReviewParser {

    private static final String OTZOVIK_BASE_URL = "https://otzovik.com";
    private static final Pattern REVIEW_ID_PATTERN = Pattern.compile("(review_\\d+)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    public List<OtzovikReviewCard> parseListPage(Document document) {
        if (document == null) {
            throw new OtzovikScrapingException("Received empty HTML document from Otzovik");
        }

        List<Element> cards = document.select("div[itemprop=review]");
        if (cards.isEmpty()) {
            throw new OtzovikScrapingException("Otzovik page does not contain any review cards");
        }

        return cards.stream()
                .map(this::toCard)
                .flatMap(Optional::stream)
                .toList();
    }

    public OtzovikFullReview parseFullReview(Document document) {
        if (document == null) {
            return new OtzovikFullReview(null, null, null, null, null, null, null);
        }
        return new OtzovikFullReview(
                firstText(document, "h1[itemprop=name]", "h1", "a.review-title[itemprop=name]"),
                firstText(document,
                        "div[itemprop=reviewBody]",
                        "[itemprop=description]",
                        ".review-body",
                        ".review-text",
                        ".review-teaser[itemprop=description]"),
                extractRating(document),
                extractPublishedAt(document),
                firstText(document, ".item-left [itemprop=name]", "[itemprop=author] [itemprop=name]", ".user-name"),
                cleanPrefixed(firstText(document, "div.review-plus", ".review-plus"), "Достоинства:"),
                cleanPrefixed(firstText(document, "div.review-minus", ".review-minus"), "Недостатки:")
        );
    }

    public ReviewCandidate toCandidate(OtzovikReviewCard card, OtzovikFullReview fullReview) {
        String text = buildReviewText(
                firstNonBlank(fullReview.title(), card.title()),
                firstNonBlank(fullReview.text(), card.teaser()),
                firstNonBlank(fullReview.pros(), card.pros()),
                firstNonBlank(fullReview.cons(), card.cons())
        );

        return new ReviewCandidate(
                "otzovik:" + card.externalId(),
                text,
                firstNonBlank(fullReview.authorName(), card.authorName()),
                firstNonNull(fullReview.rating(), card.rating()),
                firstNonNull(fullReview.publishedAt(), firstNonNull(card.publishedAt(), OffsetDateTime.now(ZoneOffset.UTC))),
                card.reviewUrl()
        );
    }

    public ReviewCandidate toFallbackCandidate(OtzovikReviewCard card) {
        return toCandidate(card, new OtzovikFullReview(null, null, null, null, null, null, null));
    }

    private Optional<OtzovikReviewCard> toCard(Element card) {
        String reviewUrl = extractReviewUrl(card);
        String externalId = extractExternalId(reviewUrl);
        if (externalId == null) {
            log.warn("Skipping Otzovik review card because review URL or external id is missing");
            return Optional.empty();
        }

        return Optional.of(new OtzovikReviewCard(
                externalId,
                reviewUrl,
                firstText(card, "a.review-title[itemprop=name]", "[itemprop=name]", "a.review-read-link"),
                firstText(card, "div.review-teaser[itemprop=description]", "[itemprop=description]", ".review-teaser"),
                extractRating(card),
                extractPublishedAt(card),
                firstText(card, ".item-left [itemprop=name]", "[itemprop=author] [itemprop=name]", ".user-name"),
                cleanPrefixed(firstText(card, "div.review-plus", ".review-plus"), "Достоинства:"),
                cleanPrefixed(firstText(card, "div.review-minus", ".review-minus"), "Недостатки:"),
                parseInteger(firstText(card, "a.review-yes span")),
                parseInteger(firstText(card, "span[itemprop=commentCount]"))
        ));
    }

    private String extractReviewUrl(Element card) {
        String url = firstAttribute(card, "meta[itemprop=url]", "content");
        if (url == null) {
            url = firstAttribute(card, "a.review-read-link", "href");
        }
        if (url == null) {
            url = firstAttribute(card, "a.review-title[itemprop=name]", "href");
        }
        return absolutize(url);
    }

    private String extractExternalId(String reviewUrl) {
        if (reviewUrl == null) {
            return null;
        }
        Matcher matcher = REVIEW_ID_PATTERN.matcher(reviewUrl);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Integer extractRating(Element root) {
        String raw = firstAttribute(root, "meta[itemprop=ratingValue]", "content");
        if (raw == null) {
            raw = firstText(root, "div.rating-score span", ".rating-score span");
        }
        return parseInteger(raw);
    }

    private OffsetDateTime extractPublishedAt(Element root) {
        String raw = firstAttribute(root, "div.review-postdate[itemprop=datePublished]", "content");
        if (raw == null) {
            raw = firstAttribute(root, "[itemprop=datePublished]", "content");
        }
        if (raw == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException exception) {
            log.warn("Failed to parse Otzovik publishedAt '{}': {}", raw, exception.getMessage());
            return null;
        }
    }

    private String buildReviewText(String title, String fullText, String pros, String cons) {
        return String.join("\n\n", java.util.stream.Stream.of(
                        normalizeText(title),
                        normalizeText(fullText),
                        prefixed("Достоинства: ", pros),
                        prefixed("Недостатки: ", cons)
                )
                .filter(value -> value != null && !value.isBlank())
                .toList());
    }

    private String firstText(Element root, String... selectors) {
        for (String selector : selectors) {
            Element element = root.selectFirst(selector);
            String text = element == null ? null : normalizeText(element.text());
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String firstAttribute(Element root, String selector, String attribute) {
        Element element = root.selectFirst(selector);
        if (element == null) {
            return null;
        }
        return normalizeText(element.attr(attribute));
    }

    private String cleanPrefixed(String value, String prefix) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        return normalized.regionMatches(true, 0, prefix, 0, prefix.length())
                ? normalizeText(normalized.substring(prefix.length()))
                : normalized;
    }

    private String prefixed(String prefix, String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : prefix + normalized;
    }

    private String absolutize(String url) {
        String normalized = normalizeText(url);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        if (normalized.startsWith("/")) {
            return OTZOVIK_BASE_URL + normalized;
        }
        return OTZOVIK_BASE_URL + "/" + normalized;
    }

    private Integer parseInteger(String raw) {
        String normalized = normalizeText(raw);
        if (normalized == null) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.valueOf(matcher.group());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = normalizeText(first);
        return normalizedFirst == null ? normalizeText(second) : normalizedFirst;
    }

    private <T> T firstNonNull(T first, T second) {
        return first == null ? second : first;
    }
}
