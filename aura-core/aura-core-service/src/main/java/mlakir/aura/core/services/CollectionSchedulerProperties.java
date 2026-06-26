package mlakir.aura.core.services;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "collection.scheduler")
public class CollectionSchedulerProperties {

    private boolean enabled = true;
    private long fixedDelayMs = 60000;
}
