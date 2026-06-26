package mlakir.aura.core.services.tabiturient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.services.ReviewCandidate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TabiturientReviewParser {

    private static final int MIN_TEXT_LENGTH = 10;
    private static final Pattern DOVERIE_ID_PATTERN = Pattern.compile("doverieform(\\d+)");
    private static final Pattern LIKE_ID_PATTERN = Pattern.compile("likediv(\\d+)");
    private static final Pattern REQUEST_RATING_PATTERN = Pattern.compile("request(\\d+)");
    private static final Map<String, Integer> MONTHS = buildMonths();

    private final TabiturientScraperProperties properties;

    public List<ReviewCandidate> parse(Document document, String sourceUrl) {
        if (document == null) {
            throw new TabiturientScrapingException("Received empty HTML document from Tabiturient");
        }

        List<Element> cards = document.select(TabiturientSelectors.REVIEW_CARD);
        if (cards.isEmpty()) {
            throw new TabiturientScrapingException("Tabiturient page does not contain any review cards");
        }

        return cards.stream()
                .limit(Math.max(properties.getMaxReviewsPerRun(), 0))
                .map(card -> toCandidate(card, sourceUrl))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ReviewCandidate> toCandidate(Element card, String sourceUrl) {
        try {
            String text = extractText(card);
            if (text == null || text.length() < MIN_TEXT_LENGTH) {
                return Optional.empty();
            }

            String reviewId = extractReviewId(card).orElseGet(() -> fallbackReviewId(sourceUrl, text));
            return Optional.of(new ReviewCandidate(
                    "tabiturient:" + reviewId,
                    text,
                    normalizeAuthor(textOf(card, TabiturientSelectors.REVIEW_AUTHOR)),
                    extractRating(card),
                    extractPublishedAt(card),
                    extractOriginalUrl(card, reviewId)
            ));
        } catch (Exception exception) {
            log.warn("Skipping unexpected Tabiturient review card due to parser error: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private String extractText(Element card) {
        Element textElement = card.selectFirst(TabiturientSelectors.REVIEW_TEXT);
        if (textElement == null) {
            return null;
        }

        Element clone = textElement.clone();
        clone.select(TabiturientSelectors.REVIEW_TEXT_EXPAND_BUTTON).remove();
        String normalized = normalizeText(clone.text())
                .replace("Показать полностью...", " ")
                .replaceAll("(^|\\s)\\.\\.\\.(\\s|$)", " ");
        normalized = normalizeText(normalized);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private Optional<String> extractReviewId(Element card) {
        String doverieId = attributeOf(card, TabiturientSelectors.REVIEW_ID_FROM_DOVERIE, "id");
        String parsedDoverieId = extractPatternGroup(doverieId, DOVERIE_ID_PATTERN);
        if (parsedDoverieId != null) {
            return Optional.of(parsedDoverieId);
        }

        String likeId = attributeOf(card, TabiturientSelectors.REVIEW_ID_FROM_LIKE, "id");
        String parsedLikeId = extractPatternGroup(likeId, LIKE_ID_PATTERN);
        if (parsedLikeId != null) {
            return Optional.of(parsedLikeId);
        }

        return Optional.empty();
    }

    private String extractOriginalUrl(Element card, String reviewId) {
        Element link = card.selectFirst(TabiturientSelectors.REVIEW_ORIGINAL_URL);
        if (link != null) {
            String absUrl = normalizeText(link.absUrl("href"));
            if (absUrl != null) {
                return absUrl;
            }
        }
        return "https://tabiturient.ru/sliv/n/?" + reviewId;
    }

    private Integer extractRating(Element card) {
        for (String className : card.classNames()) {
            String rating = extractPatternGroup(className, REQUEST_RATING_PATTERN);
            if (rating != null) {
                try {
                    return Integer.valueOf(rating);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private OffsetDateTime extractPublishedAt(Element card) {
        String rawDate = normalizeText(textOf(card, TabiturientSelectors.REVIEW_DATE));
        if (rawDate == null) {
            log.warn("Failed to parse Tabiturient review date: value is missing");
            return OffsetDateTime.now(ZoneOffset.UTC);
        }

        try {
            String[] parts = rawDate.toLowerCase().split(" ");
            if (parts.length != 3) {
                throw new DateTimeParseException("Unexpected date format", rawDate, 0);
            }
            int day = Integer.parseInt(parts[0]);
            Integer month = MONTHS.get(parts[1]);
            int year = Integer.parseInt(parts[2]);
            if (month == null) {
                throw new DateTimeParseException("Unknown month", rawDate, 0);
            }
            return LocalDate.of(year, month, day).atStartOfDay().atOffset(ZoneOffset.UTC);
        } catch (Exception exception) {
            log.warn("Failed to parse Tabiturient review date '{}': {}", rawDate, exception.getMessage());
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    private String fallbackReviewId(String sourceUrl, String text) {
        return sha256(sourceUrl + "::" + text);
    }

    static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeAuthor(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        return normalized.endsWith(":")
                ? normalizeText(normalized.substring(0, normalized.length() - 1))
                : normalized;
    }

    private String textOf(Element root, String selector) {
        Element element = root.selectFirst(selector);
        return element == null ? null : element.text();
    }

    private String attributeOf(Element root, String selector, String attribute) {
        Element element = root.selectFirst(selector);
        return element == null ? null : element.attr(attribute);
    }

    private String extractPatternGroup(String value, Pattern pattern) {
        if (value == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < 12; index++) {
                builder.append(String.format("%02x", bytes[index]));
            }
            return builder.toString();
        } catch (Exception exception) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static Map<String, Integer> buildMonths() {
        Map<String, Integer> months = new HashMap<>();
        months.put("января", 1);
        months.put("февраля", 2);
        months.put("марта", 3);
        months.put("апреля", 4);
        months.put("мая", 5);
        months.put("июня", 6);
        months.put("июля", 7);
        months.put("августа", 8);
        months.put("сентября", 9);
        months.put("октября", 10);
        months.put("ноября", 11);
        months.put("декабря", 12);
        return months;
    }
}
