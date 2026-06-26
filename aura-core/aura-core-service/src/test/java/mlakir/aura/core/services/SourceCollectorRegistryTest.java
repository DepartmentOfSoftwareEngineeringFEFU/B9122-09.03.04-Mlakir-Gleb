package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.exceptions.SourceExceptionFactory;
import mlakir.aura.core.models.SourceEntity;
import org.junit.jupiter.api.Test;

class SourceCollectorRegistryTest {

    @Test
    void shouldSelectOtzovikCollector() {
        SourceCollector otzovikCollector = new TestCollector(SourceType.OTZOVIK);
        SourceCollectorRegistry registry = new SourceCollectorRegistry(
                List.of(new TestCollector(SourceType.TABITURIENT), otzovikCollector),
                new SourceExceptionFactory()
        );

        assertSame(otzovikCollector, registry.getCollector(SourceType.OTZOVIK));
    }

    @Test
    void shouldSelectVuzopediaCollector() {
        SourceCollector vuzopediaCollector = new TestCollector(SourceType.VUZOPEDIA);
        SourceCollectorRegistry registry = new SourceCollectorRegistry(
                List.of(new TestCollector(SourceType.TABITURIENT), vuzopediaCollector),
                new SourceExceptionFactory()
        );

        assertSame(vuzopediaCollector, registry.getCollector(SourceType.VUZOPEDIA));
    }

    private record TestCollector(SourceType supportedType) implements SourceCollector {
        @Override
        public boolean supports(SourceType sourceType) {
            return sourceType == supportedType;
        }

        @Override
        public List<ReviewCandidate> collect(SourceEntity source) {
            return List.of();
        }
    }
}
