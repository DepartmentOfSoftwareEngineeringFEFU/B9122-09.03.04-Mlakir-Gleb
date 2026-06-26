package mlakir.aura.core.services;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "reviews.analysis")
public class ReviewsAnalysisProperties {

    private int maxRetries = 5;
}
