package mlakir.aura.core.services.tabiturient;

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
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TabiturientSourceCollector implements SourceCollector {

    private final SourceBaseUrlNormalizer sourceBaseUrlNormalizer;
    private final HtmlPageFetcher htmlPageFetcher;
    private final TabiturientAjaxClient tabiturientAjaxClient;
    private final TabiturientReviewParser parser;
    private final TabiturientScraperProperties properties;

    @Override
    public boolean supports(SourceType sourceType) {
        return sourceType == SourceType.TABITURIENT;
    }

    @Override
    public List<ReviewCandidate> collect(SourceEntity source) {
        if (source.getType() != SourceType.TABITURIENT) {
            throw new TabiturientScrapingException(
                    "Tabiturient collector does not support source type " + source.getType()
            );
        }

        String normalizedUrl = sourceBaseUrlNormalizer.normalizeTabiturientUrl(source.getBaseUrl());
        String vuzId = sourceBaseUrlNormalizer.extractTabiturientVuzId(normalizedUrl);
        log.info("Collecting Tabiturient reviews for sourceId={} from {} (vuzId={})",
                source.getId(), normalizedUrl, vuzId);

        Map<String, ReviewCandidate> collectedByExternalId = new LinkedHashMap<>();
        Document document = htmlPageFetcher.fetch(normalizedUrl);
        List<ReviewCandidate> initialReviews = parser.parse(document, normalizedUrl);
        addNewReviews(collectedByExternalId, initialReviews);
        log.info("Tabiturient initial page parsed for sourceId={} vuzId={}: parsed={}, new={}",
                source.getId(), vuzId, initialReviews.size(), collectedByExternalId.size());

        int pageSize = Math.max(properties.getPageSize(), 1);
        int maxReviewsPerRun = Math.max(properties.getMaxReviewsPerRun(), pageSize);
        for (int limit = pageSize; limit < maxReviewsPerRun; limit += pageSize) {
            int nextLimit = Math.min(limit + pageSize, maxReviewsPerRun);
            sleepBetweenRequestsIfNeeded();

            List<ReviewCandidate> parsedReviews;
            try {
                Document ajaxDocument = tabiturientAjaxClient.fetchReviews(vuzId, nextLimit);
                parsedReviews = parser.parse(ajaxDocument, normalizedUrl);
            } catch (Exception exception) {
                log.warn("Stopping Tabiturient AJAX pagination for sourceId={} vuzId={} limit={}: {}",
                        source.getId(), vuzId, nextLimit, exception.getMessage());
                break;
            }

            if (parsedReviews.isEmpty()) {
                log.info("Stopping Tabiturient AJAX pagination for sourceId={} vuzId={} limit={}: parser returned no reviews",
                        source.getId(), vuzId, nextLimit);
                break;
            }

            int beforeSize = collectedByExternalId.size();
            int added = addNewReviews(collectedByExternalId, parsedReviews);
            log.info("Tabiturient AJAX page parsed for sourceId={} vuzId={} limit={}: parsed={}, new={}, total={}",
                    source.getId(), vuzId, nextLimit, parsedReviews.size(), added, collectedByExternalId.size());

            if (collectedByExternalId.size() >= maxReviewsPerRun) {
                break;
            }
            if (added == 0 || collectedByExternalId.size() == beforeSize) {
                log.info("Stopping Tabiturient AJAX pagination for sourceId={} vuzId={} limit={}: no new external ids",
                        source.getId(), vuzId, nextLimit);
                break;
            }
        }

        return new ArrayList<>(collectedByExternalId.values()).stream()
                .limit(maxReviewsPerRun)
                .toList();
    }

    private int addNewReviews(Map<String, ReviewCandidate> collectedByExternalId, List<ReviewCandidate> reviews) {
        int added = 0;
        for (ReviewCandidate review : reviews) {
            if (collectedByExternalId.putIfAbsent(review.externalId(), review) == null) {
                added++;
            }
        }
        return added;
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
            throw new TabiturientScrapingException("Tabiturient AJAX pagination was interrupted", exception);
        }
    }
}
