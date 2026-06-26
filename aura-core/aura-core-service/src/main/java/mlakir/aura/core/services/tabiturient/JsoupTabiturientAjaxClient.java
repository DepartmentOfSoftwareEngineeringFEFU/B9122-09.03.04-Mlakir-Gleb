package mlakir.aura.core.services.tabiturient;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsoupTabiturientAjaxClient implements TabiturientAjaxClient {

    private final TabiturientScraperProperties properties;
    private final JsoupConnectionFactory connectionFactory;

    @Override
    public Document fetchReviews(String vuzId, int limit) {
        try {
            Connection.Response response = connectionFactory.connect(properties.getAjaxUrl())
                    .userAgent(properties.getUserAgent())
                    .timeout(properties.getTimeoutMs())
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .method(Connection.Method.POST)
                    .data("vuzid", vuzId)
                    .data("limit", String.valueOf(limit))
                    .data("sortby", "3")
                    .data("sortby2", "")
                    .execute();
            return Jsoup.parse(response.body(), "https://tabiturient.ru");
        } catch (HttpStatusException exception) {
            log.warn("Tabiturient AJAX fetch failed for vuzId={} limit={} with HTTP {}", vuzId, limit,
                    exception.getStatusCode());
            throw new TabiturientScrapingException(
                    "Failed to fetch Tabiturient AJAX reviews: HTTP " + exception.getStatusCode(),
                    exception
            );
        } catch (IOException exception) {
            log.warn("Tabiturient AJAX fetch failed for vuzId={} limit={}: {}", vuzId, limit, exception.getMessage());
            throw new TabiturientScrapingException(
                    "Failed to fetch Tabiturient AJAX reviews: " + exception.getMessage(),
                    exception
            );
        }
    }
}
