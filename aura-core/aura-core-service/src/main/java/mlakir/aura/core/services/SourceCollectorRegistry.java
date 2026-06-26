package mlakir.aura.core.services;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.exceptions.SourceExceptionFactory;
import org.springframework.stereotype.Component;

@Component
public class SourceCollectorRegistry {

    private final Map<SourceType, SourceCollector> collectorsByType;
    private final SourceExceptionFactory sourceExceptionFactory;

    public SourceCollectorRegistry(List<SourceCollector> collectors, SourceExceptionFactory sourceExceptionFactory) {
        this.sourceExceptionFactory = sourceExceptionFactory;
        this.collectorsByType = new EnumMap<>(SourceType.class);
        for (SourceCollector collector : collectors) {
            for (SourceType sourceType : SourceType.values()) {
                if (collector.supports(sourceType)) {
                    collectorsByType.put(sourceType, collector);
                }
            }
        }
    }

    public SourceCollector getCollector(SourceType sourceType) {
        SourceCollector collector = collectorsByType.get(sourceType);
        if (collector == null) {
            throw sourceExceptionFactory.unsupportedSourceType(sourceType);
        }
        return collector;
    }
}
