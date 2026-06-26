package mlakir.aura.core.services.tabiturient;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsoupHtmlPageFetcher implements HtmlPageFetcher {

    private final TabiturientScraperProperties properties;

    @Override
    public Document fetch(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(properties.getUserAgent())
                    .timeout(properties.getTimeoutMs())
                    .get();
        } catch (HttpStatusException exception) {
            log.warn("Tabiturient fetch failed for {} with HTTP {}", url, exception.getStatusCode());
            throw new TabiturientScrapingException(
                    "Failed to fetch Tabiturient page: HTTP " + exception.getStatusCode(),
                    exception
            );
        } catch (IOException exception) {
            log.warn("Tabiturient fetch failed for {}: {}", url, exception.getMessage());
            throw new TabiturientScrapingException("Failed to fetch Tabiturient page: " + exception.getMessage(),
                    exception);
        }
    }
}
