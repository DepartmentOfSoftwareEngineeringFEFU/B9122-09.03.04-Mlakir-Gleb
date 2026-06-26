package mlakir.aura.core.services.otzovik;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.services.tabiturient.JsoupConnectionFactory;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtzovikHtmlPageFetcher {

    private final JsoupConnectionFactory jsoupConnectionFactory;
    private final OtzovikScraperProperties properties;

    public Document fetch(String url) {
        try {
            return jsoupConnectionFactory.connect(url)
                    .userAgent(properties.getUserAgent())
                    .timeout(properties.getTimeoutMs())
                    .get();
        } catch (HttpStatusException exception) {
            log.warn("Otzovik fetch failed for {} with HTTP {}", url, exception.getStatusCode());
            throw new OtzovikScrapingException("Failed to fetch Otzovik page: HTTP " + exception.getStatusCode(),
                    exception);
        } catch (IOException exception) {
            log.warn("Otzovik fetch failed for {}: {}", url, exception.getMessage());
            throw new OtzovikScrapingException("Failed to fetch Otzovik page: " + exception.getMessage(), exception);
        }
    }
}
