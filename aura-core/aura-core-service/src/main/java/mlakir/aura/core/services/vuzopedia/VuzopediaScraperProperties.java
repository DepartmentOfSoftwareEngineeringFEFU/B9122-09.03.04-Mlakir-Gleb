package mlakir.aura.core.services.vuzopedia;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "collectors.vuzopedia")
public class VuzopediaScraperProperties {

    private int maxReviewsPerRun = 50;
    private long requestDelayMs = 500;
    private String userAgent = "Mozilla/5.0";
    private int timeoutMs = 10000;
}
