package mlakir.aura.core.services;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "reviews.reanalysis")
public class ReviewsReanalysisProperties {

    private boolean enabled;
    private long fixedDelayMs = 300000;
    private int batchSize = 100;
}
