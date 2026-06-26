package mlakir.aura.core.services.tabiturient;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class JsoupConnectionFactory {

    public Connection connect(String url) {
        return Jsoup.connect(url);
    }
}
