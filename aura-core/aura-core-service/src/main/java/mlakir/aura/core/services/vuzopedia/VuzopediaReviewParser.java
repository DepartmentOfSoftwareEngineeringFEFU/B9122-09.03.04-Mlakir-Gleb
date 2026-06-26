package mlakir.aura.core.services.vuzopedia;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VuzopediaReviewParser {

    private static final Pattern INFO_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}),\\s*от\\s+(.+)$");
    private static final String DISCLAIMER =
            "Отзыв является личным мнением автора и может не соответствовать действительности";

    public List<VuzopediaReviewData> parse(Document document, String vuzId) {
        if (document == null) {
            throw new VuzopediaScrapingException("Received empty HTML document from Vuzopedia");
        }

        List<Element> cards = document.select("div.otzivItem");
        if (cards.isEmpty()) {
            throw new VuzopediaScrapingException("Vuzopedia page does not contain any review cards");
        }

        return cards.stream()
                .map(card -> parseCard(card, vuzId))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<VuzopediaReviewData> parseCard(Element card, String vuzId) {
        try {
            String text = extractReviewText(card);
            if (text == null) {
                return Optional.empty();
            }

            ReviewInfo info = extractReviewInfo(card);
            OffsetDateTime publishedAt = info.publishedAt() == null
                    ? OffsetDateTime.now(ZoneOffset.UTC)
                    : info.publishedAt();
            String publishedKey = info.publishedAt() == null ? "unknown" : info.publishedAt().toLocalDate().toString();
            String externalId = "vuzopedia:" + vuzId + ":" + publishedKey + ":" + sha256(text, 16);
            VuzopediaSentimentHint sentimentHint = extractSentimentHint(card);
            if (sentimentHint != null) {
                log.debug("Parsed Vuzopedia source sentiment hint: externalId={}, hint={}", externalId, sentimentHint);
            }

            return Optional.of(new VuzopediaReviewData(
                    externalId,
                    text,
                    publishedAt,
                    info.authorName(),
                    sentimentHint
            ));
        } catch (Exception exception) {
            log.warn("Skipping unexpected Vuzopedia review card due to parser error: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private String extractReviewText(Element card) {
        Element clone = card.clone();
        clone.select("div.otzPredAns").remove();

        String text = normalizeText(clone.text());
        String infoText = textOf(clone, "div.otzivInfo");
        if (text != null && infoText != null) {
            int infoIndex = text.indexOf(infoText);
            if (infoIndex >= 0) {
                text = normalizeText(text.substring(0, infoIndex));
            }
        }
        if (text == null) {
            return null;
        }
        text = normalizeText(text.replace(DISCLAIMER, ""));
        return text == null || text.isBlank() ? null : text;
    }

    private ReviewInfo extractReviewInfo(Element card) {
        String rawInfo = textOf(card, "div.otzivInfo");
        if (rawInfo == null) {
            return new ReviewInfo(null, null);
        }

        Matcher matcher = INFO_PATTERN.matcher(rawInfo);
        if (!matcher.find()) {
            return new ReviewInfo(null, normalizeAuthor(rawInfo));
        }

        OffsetDateTime publishedAt = LocalDate.parse(matcher.group(1)).atStartOfDay().atOffset(ZoneOffset.UTC);
        return new ReviewInfo(publishedAt, normalizeAuthor(matcher.group(2)));
    }

    private VuzopediaSentimentHint extractSentimentHint(Element card) {
        String style = card.attr("style");
        String normalized = style == null ? "" : style.toLowerCase();
        if (normalized.contains("#4caf50")) {
            return VuzopediaSentimentHint.POSITIVE;
        }
        if (normalized.contains("#f44336")) {
            return VuzopediaSentimentHint.NEGATIVE;
        }
        if (normalized.contains("#607d8b")) {
            return VuzopediaSentimentHint.NEUTRAL;
        }
        return null;
    }

    private String textOf(Element root, String selector) {
        Element element = root.selectFirst(selector);
        return element == null ? null : normalizeText(element.text());
    }

    private String normalizeAuthor(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        return normalized.equalsIgnoreCase("анонима") ? "аноним" : normalized;
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

    private String sha256(String value, int hexLength) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
                if (builder.length() >= hexLength) {
                    return builder.substring(0, hexLength);
                }
            }
            return builder.toString();
        } catch (Exception exception) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private record ReviewInfo(OffsetDateTime publishedAt, String authorName) {
    }
}
