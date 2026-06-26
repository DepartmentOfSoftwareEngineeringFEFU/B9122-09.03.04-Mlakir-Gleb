package mlakir.aura.core.services.tabiturient;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tabiturient.scraper")
public class TabiturientScraperProperties {

    private String userAgent = "AuraReviewBot/1.0";
    private int timeoutMs = 10000;
    private int pageSize = 25;
    private int maxReviewsPerRun = 100;
    private String ajaxUrl = "https://tabiturient.ru/ajax/ajsliv.php";
    private long requestDelayMs = 500;
}
