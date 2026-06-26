package mlakir.aura.core.services;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.enums.CollectionJobStatus;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.CollectionJobRepository;
import mlakir.aura.core.repositories.SourceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionScheduler {

    private final CollectionSchedulerProperties properties;
    private final SourceRepository sourceRepository;
    private final CollectionJobRepository collectionJobRepository;
    private final CollectionService collectionService;

    @Scheduled(fixedDelayString = "${collection.scheduler.fixed-delay-ms:60000}")
    public void runDueCollections() {
        if (!properties.isEnabled()) {
            return;
        }

        List<SourceEntity> dueSources = sourceRepository
                .findByScheduleEnabledTrueAndNextCollectionAtLessThanEqual(OffsetDateTime.now());
        for (SourceEntity source : dueSources) {
            if (!Boolean.TRUE.equals(source.getScheduleEnabled())) {
                continue;
            }
            if (source.getNextCollectionAt() == null) {
                continue;
            }
            if (collectionJobRepository.existsBySourceIdAndStatus(source.getId(), CollectionJobStatus.RUNNING)) {
                log.info("Skipping scheduled collection for sourceId={} because a job is already running",
                        source.getId());
                continue;
            }
            try {
                collectionService.runScheduled(source.getId());
            } catch (Exception exception) {
                log.warn("Scheduled collection failed for sourceId={}: {}", source.getId(), exception.getMessage());
            }
        }
    }
}
