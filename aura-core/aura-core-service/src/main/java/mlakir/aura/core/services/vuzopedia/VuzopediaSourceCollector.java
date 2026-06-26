package mlakir.aura.core.services.vuzopedia;

import java.util.List;
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
public class VuzopediaSourceCollector implements SourceCollector {

    private final SourceBaseUrlNormalizer sourceBaseUrlNormalizer;
    private final VuzopediaHtmlPageFetcher htmlPageFetcher;
    private final VuzopediaReviewParser parser;
    private final VuzopediaScraperProperties properties;

    @Override
    public boolean supports(SourceType sourceType) {
        return sourceType == SourceType.VUZOPEDIA;
    }

    @Override
    public List<ReviewCandidate> collect(SourceEntity source) {
        if (source.getType() != SourceType.VUZOPEDIA) {
            throw new VuzopediaScrapingException(
                    "Vuzopedia collector does not support source type " + source.getType()
            );
        }

        String normalizedUrl = sourceBaseUrlNormalizer.normalizeVuzopediaUrl(source.getBaseUrl());
        String vuzId = sourceBaseUrlNormalizer.extractVuzopediaVuzId(normalizedUrl);
        log.info("Collecting Vuzopedia reviews for sourceId={} from {} (vuzId={})",
                source.getId(), normalizedUrl, vuzId);

        sleepBeforeRequestIfNeeded();
        Document document = htmlPageFetcher.fetch(buildPageUrl(normalizedUrl, 1));
        int maxReviewsPerRun = Math.max(properties.getMaxReviewsPerRun(), 0);
        return parser.parse(document, vuzId).stream()
                .limit(maxReviewsPerRun)
                .map(item -> new ReviewCandidate(
                        item.externalId(),
                        item.text(),
                        item.authorName(),
                        null,
                        item.publishedAt(),
                        normalizedUrl
                ))
                .toList();
    }

    String buildPageUrl(String baseUrl, int pageNumber) {
        // TODO: Extend pagination if Vuzopedia exposes stable page URLs.
        return baseUrl;
    }

    private void sleepBeforeRequestIfNeeded() {
        long delayMs = properties.getRequestDelayMs();
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new VuzopediaScrapingException("Vuzopedia review loading was interrupted", exception);
        }
    }
}
