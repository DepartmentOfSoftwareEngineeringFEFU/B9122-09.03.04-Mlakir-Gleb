package mlakir.aura.core.services;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.SourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SourceCollectionStateService {

    private final SourceRepository sourceRepository;

    @Transactional
    public void markManualCollectionFinished(Long sourceId, OffsetDateTime finishedAt) {
        SourceEntity source = sourceRepository.findById(sourceId).orElseThrow();
        source.setLastCollectedAt(finishedAt);
        sourceRepository.save(source);
    }

    @Transactional
    public void markScheduledCollectionFinished(Long sourceId, OffsetDateTime finishedAt) {
        SourceEntity source = sourceRepository.findById(sourceId).orElseThrow();
        source.setLastCollectedAt(finishedAt);
        if (Boolean.TRUE.equals(source.getScheduleEnabled()) && source.getScheduleIntervalMinutes() != null) {
            source.setNextCollectionAt(finishedAt.plusMinutes(source.getScheduleIntervalMinutes()));
        }
        sourceRepository.save(source);
    }
}
