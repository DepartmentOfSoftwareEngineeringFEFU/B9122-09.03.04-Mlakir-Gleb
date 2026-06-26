package mlakir.aura.core.services.otzovik;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "collectors.otzovik")
public class OtzovikScraperProperties {

    private int maxReviewsPerRun = 50;
    private long requestDelayMs = 500;
    private String userAgent = "Mozilla/5.0";
    private int timeoutMs = 10000;
}
