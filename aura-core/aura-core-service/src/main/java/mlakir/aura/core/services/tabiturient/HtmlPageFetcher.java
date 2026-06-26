package mlakir.aura.core.services.tabiturient;

import org.jsoup.nodes.Document;

public interface HtmlPageFetcher {

    Document fetch(String url);
}
