package mlakir.aura.core.services;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.dto.ReviewReanalysisResponseDto;
import mlakir.aura.core.enums.CollectionJobStatus;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.repositories.CollectionJobRepository;
import mlakir.aura.core.repositories.ReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewReanalysisService {

    private static final int DEFAULT_LIMIT = 100;

    private final ReviewRepository reviewRepository;
    private final ReviewBatchAnalysisService reviewBatchAnalysisService;
    private final ReviewsAnalysisProperties reviewsAnalysisProperties;
    private final CollectionJobRepository collectionJobRepository;

    public ReviewReanalysisResponseDto reanalyzeFailedReviews(Long organizationId,
                                                              Long sourceId,
                                                              Integer limit,
                                                              boolean force) {
        int batchSize = normalizeLimit(limit);
        int maxRetries = reviewsAnalysisProperties.getMaxRetries();
        int skippedCount = force
                ? 0
                : safeLongToInt(reviewRepository.countSkippedForReanalysis(
                ReviewStatus.FAILED_ANALYSIS, organizationId, sourceId, maxRetries
        ));
        List<ReviewEntity> reviewsForReanalysis = reviewRepository.findForReanalysis(
                statusesForReanalysis(force),
                organizationId,
                sourceId,
                force,
                maxRetries,
                PageRequest.of(0, batchSize)
        );

        if (reviewsForReanalysis.isEmpty()) {
            return new ReviewReanalysisResponseDto(organizationId, sourceId, 0, 0, 0, skippedCount, null);
        }

        int requestedCount = reviewsForReanalysis.size();
        try {
            reviewBatchAnalysisService.analyzeReviews(reviewsForReanalysis);
        } catch (Exception exception) {
            String errorMessage = safeErrorMessage(exception);
            log.warn("Review reanalysis failed for organizationId={} sourceId={} count={}: {}",
                    organizationId, sourceId, requestedCount, errorMessage);
            markPreviouslyFailedReviewsAsFailed(reviewsForReanalysis, errorMessage);
            return new ReviewReanalysisResponseDto(
                    organizationId,
                    sourceId,
                    requestedCount,
                    0,
                    requestedCount,
                    skippedCount,
                    errorMessage
            );
        }

        resolveCollectionJobsIfSourceFailuresCleared(reviewsForReanalysis);
        return new ReviewReanalysisResponseDto(
                organizationId,
                sourceId,
                requestedCount,
                requestedCount,
                0,
                skippedCount,
                null
        );
    }

    private List<ReviewStatus> statusesForReanalysis(boolean force) {
        if (force) {
            return List.of(ReviewStatus.FAILED_ANALYSIS, ReviewStatus.ANALYZED);
        }
        return List.of(ReviewStatus.FAILED_ANALYSIS);
    }

    private void markPreviouslyFailedReviewsAsFailed(List<ReviewEntity> reviews, String errorMessage) {
        List<ReviewEntity> previouslyFailedReviews = reviews.stream()
                .filter(review -> review.getStatus() == ReviewStatus.FAILED_ANALYSIS)
                .toList();
        if (previouslyFailedReviews.isEmpty()) {
            return;
        }
        reviewBatchAnalysisService.markFailedAnalysis(previouslyFailedReviews, errorMessage);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return limit;
    }

    private String safeErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "analysis-service unavailable";
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }

    private void resolveCollectionJobsIfSourceFailuresCleared(List<ReviewEntity> reanalyzedReviews) {
        reanalyzedReviews.stream()
                .map(ReviewEntity::getSource)
                .filter(Objects::nonNull)
                .map(source -> source.getId())
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::resolveLatestFailedCollectionJobIfSourceHasNoFailedAnalysis);
    }

    private void resolveLatestFailedCollectionJobIfSourceHasNoFailedAnalysis(Long sourceId) {
        try {
            long remainingFailedReviews = reviewRepository.countBySourceIdAndStatus(
                    sourceId,
                    ReviewStatus.FAILED_ANALYSIS
            );
            if (remainingFailedReviews > 0) {
                return;
            }

            collectionJobRepository.findFirstBySourceIdAndStatusOrderByStartedAtDescIdDesc(
                    sourceId,
                    CollectionJobStatus.FAILED
            ).ifPresent(job -> {
                job.setStatus(CollectionJobStatus.SUCCESS);
                job.setErrorMessage(null);
                collectionJobRepository.save(job);
            });
        } catch (Exception exception) {
            log.warn("Failed to resolve collection job after successful review reanalysis for sourceId={}: {}",
                    sourceId, safeErrorMessage(exception));
        }
    }

    private int safeLongToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
