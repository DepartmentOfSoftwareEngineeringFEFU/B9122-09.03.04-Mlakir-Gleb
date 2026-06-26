package mlakir.aura.core.services;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.enums.CollectionJobStatus;
import mlakir.aura.core.models.CollectionJobEntity;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.CollectionJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollectionJobLifecycleService {

    private final CollectionJobRepository collectionJobRepository;

    @Transactional
    public CollectionJobEntity createRunningJob(SourceEntity source, String triggeredBy) {
        CollectionJobEntity job = new CollectionJobEntity();
        job.setSource(source);
        job.setStatus(CollectionJobStatus.RUNNING);
        job.setStartedAt(OffsetDateTime.now());
        job.setCollectedCount(0);
        job.setTriggeredBy(triggeredBy);
        return fetchDetailed(collectionJobRepository.save(job));
    }

    @Transactional
    public CollectionJobEntity finishSuccessfully(CollectionJobEntity job, int collectedCount) {
        job.setStatus(CollectionJobStatus.SUCCESS);
        job.setCollectedCount(collectedCount);
        job.setFinishedAt(OffsetDateTime.now());
        job.setErrorMessage(null);
        return fetchDetailed(collectionJobRepository.save(job));
    }

    @Transactional
    public CollectionJobEntity finishFailed(CollectionJobEntity job, Exception exception) {
        job.setStatus(CollectionJobStatus.FAILED);
        job.setFinishedAt(OffsetDateTime.now());
        job.setErrorMessage(truncateErrorMessage(resolveErrorMessage(exception)));
        return fetchDetailed(collectionJobRepository.save(job));
    }

    private CollectionJobEntity fetchDetailed(CollectionJobEntity job) {
        CollectionJobEntity detailed = collectionJobRepository.findDetailedById(job.getId()).orElse(job);
        if (detailed.getSource() != null) {
            detailed.getSource().getId();
            detailed.getSource().getName();
        }
        return detailed;
    }

    private String truncateErrorMessage(String message) {
        if (message == null || message.length() <= 2000) {
            return message;
        }
        return message.substring(0, 2000);
    }

    private String resolveErrorMessage(Exception exception) {
        if (exception == null) {
            return "Сбор завершился с ошибкой. Подробности отсутствуют.";
        }

        String message = exception.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }

        return "Сбор завершился с ошибкой. Причина: " + exception.getClass().getSimpleName();
    }
}
