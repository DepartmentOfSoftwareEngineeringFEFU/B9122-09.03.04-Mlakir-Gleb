package mlakir.aura.core.integrations.analysis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "analysis")
public class AnalysisProperties {

    private int maxTextLength = 10000;
}
