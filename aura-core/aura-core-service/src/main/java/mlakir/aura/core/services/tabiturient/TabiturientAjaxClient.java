package mlakir.aura.core.services.tabiturient;

import org.jsoup.nodes.Document;

public interface TabiturientAjaxClient {

    Document fetchReviews(String vuzId, int limit);
}
