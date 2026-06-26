package mlakir.aura.core.services.otzovik;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.services.ReviewCandidate;
import mlakir.aura.core.services.SourceCollector;
import mlakir.aura.core.services.tabiturient.SourceBaseUrlNormalizer;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtzovikSourceCollector implements SourceCollector {

    private final SourceBaseUrlNormalizer sourceBaseUrlNormalizer;
    private final OtzovikHtmlPageFetcher htmlPageFetcher;
    private final OtzovikReviewParser parser;
    private final OtzovikScraperProperties properties;

    @Override
    public boolean supports(SourceType sourceType) {
        return sourceType == SourceType.OTZOVIK;
    }

    @Override
    public List<ReviewCandidate> collect(SourceEntity source) {
        if (source.getType() != SourceType.OTZOVIK) {
            throw new OtzovikScrapingException("Otzovik collector does not support source type " + source.getType());
        }

        String normalizedUrl = sourceBaseUrlNormalizer.normalizeOtzovikUrl(source.getBaseUrl());
        log.info("Collecting Otzovik reviews for sourceId={} from {}", source.getId(), normalizedUrl);

        Document listDocument = htmlPageFetcher.fetch(buildPageUrl(normalizedUrl, 1));
        List<OtzovikReviewCard> cards = parser.parseListPage(listDocument);
        int maxReviewsPerRun = Math.max(properties.getMaxReviewsPerRun(), 0);
        Map<String, ReviewCandidate> collectedByExternalId = new LinkedHashMap<>();

        for (OtzovikReviewCard card : cards.stream().limit(maxReviewsPerRun).toList()) {
            ReviewCandidate candidate = fetchFullReviewCandidate(card);
            collectedByExternalId.putIfAbsent(candidate.externalId(), candidate);
        }

        return new ArrayList<>(collectedByExternalId.values());
    }

    String buildPageUrl(String baseUrl, int pageNumber) {
        // TODO: Extend pagination beyond the first page when Otzovik URL patterns are confirmed.
        return baseUrl;
    }

    private ReviewCandidate fetchFullReviewCandidate(OtzovikReviewCard card) {
        sleepBetweenRequestsIfNeeded();
        try {
            Document fullDocument = htmlPageFetcher.fetch(card.reviewUrl());
            return parser.toCandidate(card, parser.parseFullReview(fullDocument));
        } catch (Exception exception) {
            log.warn("Failed to fetch Otzovik full review {}: {}", card.reviewUrl(), exception.getMessage());
            return parser.toFallbackCandidate(card);
        }
    }

    private void sleepBetweenRequestsIfNeeded() {
        long delayMs = properties.getRequestDelayMs();
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OtzovikScrapingException("Otzovik review loading was interrupted", exception);
        }
    }
}
