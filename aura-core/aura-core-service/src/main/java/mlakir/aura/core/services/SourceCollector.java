package mlakir.aura.core.services;

import java.util.List;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.models.SourceEntity;

public interface SourceCollector {

    boolean supports(SourceType sourceType);

    List<ReviewCandidate> collect(SourceEntity source);
}
