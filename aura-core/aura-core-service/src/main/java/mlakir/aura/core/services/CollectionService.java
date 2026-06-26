package mlakir.aura.core.services;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.dto.CollectionJobResponseDto;
import mlakir.aura.core.enums.CollectionJobStatus;
import mlakir.aura.core.exceptions.CollectionExceptionFactory;
import mlakir.aura.core.exceptions.SourceExceptionFactory;
import mlakir.aura.core.mappers.CollectionJobMapper;
import mlakir.aura.core.mappers.ReviewCandidateMapper;
import mlakir.aura.core.models.CollectionJobEntity;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.CollectionJobRepository;
import mlakir.aura.core.repositories.ReviewRepository;
import mlakir.aura.core.security.CurrentUserProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectionService {

    private final SourceService sourceService;
    private final SourceExceptionFactory sourceExceptionFactory;
    private final CollectionJobRepository collectionJobRepository;
    private final CollectionJobMapper collectionJobMapper;
    private final CollectionExceptionFactory collectionExceptionFactory;
    private final CollectionJobLifecycleService collectionJobLifecycleService;
    private final ReviewRepository reviewRepository;
    private final SourceCollectorRegistry sourceCollectorRegistry;
    private final ReviewCandidateMapper reviewCandidateMapper;
    private final ReviewBatchAnalysisService reviewBatchAnalysisService;
    private final CurrentUserProvider currentUserProvider;
    private final SourceCollectionStateService sourceCollectionStateService;

    public CollectionJobResponseDto run(Long sourceId) {
        return runInternal(sourceId, currentUserProvider.getCurrentUsername(), false);
    }

    public CollectionJobResponseDto runScheduled(Long sourceId) {
        return runInternal(sourceId, "SYSTEM_SCHEDULER", true);
    }

    private CollectionJobResponseDto runInternal(Long sourceId, String triggeredBy, boolean scheduledRun) {
        SourceEntity source = sourceService.getSourceOrThrow(sourceId);
        validateSource(source);
        validateNoActiveJob(sourceId);

        CollectionJobEntity job = collectionJobLifecycleService.createRunningJob(source, triggeredBy);
        try {
            int collectedCount = collectReviews(source);
            OffsetDateTime finishedAt = OffsetDateTime.now();
            updateSourceCollectionState(sourceId, finishedAt, scheduledRun);
            job = collectionJobLifecycleService.finishSuccessfully(job, collectedCount);
        } catch (Exception exception) {
            log.error("Collection job {} failed for source {}", job.getId(), sourceId, exception);
            updateSourceCollectionState(sourceId, OffsetDateTime.now(), scheduledRun);
            job = collectionJobLifecycleService.finishFailed(job, exception);
        }

        return collectionJobMapper.toDto(job);
    }

    @Transactional(readOnly = true)
    public List<CollectionJobResponseDto> findLatestJobs(int limit) {
        int sanitizedLimit = Math.max(1, Math.min(limit, 100));
        return collectionJobRepository.findAllByOrderByStartedAtDescIdDesc(PageRequest.of(0, sanitizedLimit)).stream()
                .map(collectionJobMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CollectionJobResponseDto findJobById(Long jobId) {
        CollectionJobEntity entity = collectionJobRepository.findDetailedById(jobId)
                .orElseThrow(() -> collectionExceptionFactory.collectionJobNotFound(jobId));
        return collectionJobMapper.toDto(entity);
    }

    private void validateSource(SourceEntity source) {
        if (!Boolean.TRUE.equals(source.getIsActive())) {
            throw sourceExceptionFactory.sourceInactive(source.getId());
        }
    }

    private void validateNoActiveJob(Long sourceId) {
        if (collectionJobRepository.existsBySourceIdAndStatus(sourceId, CollectionJobStatus.RUNNING)) {
            throw collectionExceptionFactory.collectionJobAlreadyRunning(sourceId);
        }
    }

    private void updateSourceCollectionState(Long sourceId, OffsetDateTime finishedAt, boolean scheduledRun) {
        if (scheduledRun) {
            sourceCollectionStateService.markScheduledCollectionFinished(sourceId, finishedAt);
            return;
        }
        sourceCollectionStateService.markManualCollectionFinished(sourceId, finishedAt);
    }

    int collectReviews(SourceEntity source) {
        List<ReviewEntity> savedReviews = new ArrayList<>();
        for (ReviewCandidate candidate : sourceCollectorRegistry.getCollector(source.getType()).collect(source)) {
            if (reviewRepository.existsBySourceIdAndExternalId(source.getId(), candidate.externalId())) {
                continue;
            }

            ReviewEntity review = reviewCandidateMapper.toEntity(source, candidate);

            try {
                savedReviews.add(reviewRepository.save(review));
            } catch (DataIntegrityViolationException exception) {
                // Duplicate external ids are safely ignored when jobs overlap or rerun.
            }
        }

        if (savedReviews.isEmpty()) {
            return 0;
        }

        try {
            reviewBatchAnalysisService.analyzeReviews(savedReviews);
            return savedReviews.size();
        } catch (Exception exception) {
            reviewBatchAnalysisService.markFailedAnalysis(savedReviews, truncateErrorMessage(exception.getMessage()));
            throw exception;
        }
    }

    private String truncateErrorMessage(String message) {
        if (message == null || message.length() <= 2000) {
            return message;
        }
        return message.substring(0, 2000);
    }
}
