package mlakir.aura.core.services;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import mlakir.aura.core.dto.ReviewReanalysisResponseDto;
import mlakir.aura.core.enums.CollectionJobStatus;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.models.CollectionJobEntity;
import mlakir.aura.core.models.OrganizationEntity;
import mlakir.aura.core.models.ReviewAnalysisEntity;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.CollectionJobRepository;
import mlakir.aura.core.repositories.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ReviewReanalysisServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewBatchAnalysisService reviewBatchAnalysisService;
    @Mock
    private CollectionJobRepository collectionJobRepository;

    private ReviewReanalysisService reviewReanalysisService;

    @BeforeEach
    void setUp() {
        ReviewsAnalysisProperties reviewsAnalysisProperties = new ReviewsAnalysisProperties();
        reviewsAnalysisProperties.setMaxRetries(5);
        reviewReanalysisService = new ReviewReanalysisService(
                reviewRepository,
                reviewBatchAnalysisService,
                reviewsAnalysisProperties,
                collectionJobRepository
        );
    }

    @Test
    void shouldReanalyzeFailedReviewsSuccessfully() {
        ReviewEntity review = failedReview(1L, 10L, 20L, 1);

        when(reviewRepository.countSkippedForReanalysis(ReviewStatus.FAILED_ANALYSIS, 10L, 20L, 5)).thenReturn(0L);
        when(reviewRepository.findForReanalysis(
                eq(List.of(ReviewStatus.FAILED_ANALYSIS)),
                eq(10L),
                eq(20L),
                eq(false),
                eq(5),
                any(Pageable.class)
        )).thenReturn(List.of(review));
        when(reviewRepository.countBySourceIdAndStatus(20L, ReviewStatus.FAILED_ANALYSIS)).thenReturn(1L);

        ReviewReanalysisResponseDto response = reviewReanalysisService.reanalyzeFailedReviews(10L, 20L, 100, false);

        assertEquals(1, response.requestedCount());
        assertEquals(1, response.reanalyzedCount());
        assertEquals(0, response.failedCount());
        assertEquals(0, response.skippedCount());
        verify(reviewBatchAnalysisService).analyzeReviews(List.of(review));
    }

    @Test
    void shouldResolveLatestFailedCollectionJobWhenSourceHasNoFailedReviewsAfterReanalysis() {
        ReviewEntity review = failedReview(1L, 10L, 20L, 1);
        CollectionJobEntity failedJob = new CollectionJobEntity();
        failedJob.setId(100L);
        failedJob.setStatus(CollectionJobStatus.FAILED);
        failedJob.setErrorMessage("analysis-service unavailable");

        when(reviewRepository.countSkippedForReanalysis(ReviewStatus.FAILED_ANALYSIS, 10L, 20L, 5)).thenReturn(0L);
        when(reviewRepository.findForReanalysis(
                eq(List.of(ReviewStatus.FAILED_ANALYSIS)),
                eq(10L),
                eq(20L),
                eq(false),
                eq(5),
                any(Pageable.class)
        )).thenReturn(List.of(review));
        when(reviewRepository.countBySourceIdAndStatus(20L, ReviewStatus.FAILED_ANALYSIS)).thenReturn(0L);
        when(collectionJobRepository.findFirstBySourceIdAndStatusOrderByStartedAtDescIdDesc(
                20L,
                CollectionJobStatus.FAILED
        )).thenReturn(java.util.Optional.of(failedJob));

        reviewReanalysisService.reanalyzeFailedReviews(10L, 20L, 100, false);

        assertEquals(CollectionJobStatus.SUCCESS, failedJob.getStatus());
        assertEquals(null, failedJob.getErrorMessage());
        verify(collectionJobRepository).save(failedJob);
    }

    @Test
    void shouldKeepCollectionJobFailedWhenSourceStillHasFailedReviewsAfterReanalysis() {
        ReviewEntity review = failedReview(1L, 10L, 20L, 1);

        when(reviewRepository.countSkippedForReanalysis(ReviewStatus.FAILED_ANALYSIS, 10L, 20L, 5)).thenReturn(0L);
        when(reviewRepository.findForReanalysis(
                eq(List.of(ReviewStatus.FAILED_ANALYSIS)),
                eq(10L),
                eq(20L),
                eq(false),
                eq(5),
                any(Pageable.class)
        )).thenReturn(List.of(review));
        when(reviewRepository.countBySourceIdAndStatus(20L, ReviewStatus.FAILED_ANALYSIS)).thenReturn(2L);

        reviewReanalysisService.reanalyzeFailedReviews(10L, 20L, 100, false);

        org.mockito.Mockito.verifyNoInteractions(collectionJobRepository);
    }

    @Test
    void shouldKeepFailedStatusWhenReanalysisFailsAgain() {
        ReviewEntity review = failedReview(2L, 10L, 20L, 2);

        when(reviewRepository.countSkippedForReanalysis(ReviewStatus.FAILED_ANALYSIS, 10L, 20L, 5)).thenReturn(0L);
        when(reviewRepository.findForReanalysis(
                eq(List.of(ReviewStatus.FAILED_ANALYSIS)),
                eq(10L),
                eq(20L),
                eq(false),
                eq(5),
                any(Pageable.class)
        )).thenReturn(List.of(review));
        org.mockito.Mockito.doThrow(new RuntimeException("analysis-service unavailable"))
                .when(reviewBatchAnalysisService).analyzeReviews(List.of(review));

        ReviewReanalysisResponseDto response = reviewReanalysisService.reanalyzeFailedReviews(10L, 20L, 100, false);

        assertEquals(1, response.requestedCount());
        assertEquals(0, response.reanalyzedCount());
        assertEquals(1, response.failedCount());
        assertEquals("analysis-service unavailable", response.errorMessage());
        verify(reviewBatchAnalysisService).markFailedAnalysis(List.of(review), "analysis-service unavailable");
    }

    @Test
    void shouldReturnSkippedCountForReviewsOverRetryLimit() {
        when(reviewRepository.countSkippedForReanalysis(ReviewStatus.FAILED_ANALYSIS, 10L, null, 5)).thenReturn(3L);
        when(reviewRepository.findForReanalysis(
                eq(List.of(ReviewStatus.FAILED_ANALYSIS)),
                eq(10L),
                eq(null),
                eq(false),
                eq(5),
                any(Pageable.class)
        )).thenReturn(List.of());

        ReviewReanalysisResponseDto response = reviewReanalysisService.reanalyzeFailedReviews(10L, null, 100, false);

        assertEquals(0, response.requestedCount());
        assertEquals(3, response.skippedCount());
    }

    @Test
    void shouldIgnoreRetryLimitWhenForceIsEnabled() {
        ReviewEntity review = failedReview(3L, 10L, 20L, 7);

        when(reviewRepository.findForReanalysis(
                eq(List.of(ReviewStatus.FAILED_ANALYSIS, ReviewStatus.ANALYZED)),
                eq(10L),
                eq(20L),
                eq(true),
                eq(5),
                any(Pageable.class)
        )).thenReturn(List.of(review));
        when(reviewRepository.countBySourceIdAndStatus(20L, ReviewStatus.FAILED_ANALYSIS)).thenReturn(1L);

        ReviewReanalysisResponseDto response = reviewReanalysisService.reanalyzeFailedReviews(10L, 20L, 100, true);

        assertEquals(1, response.requestedCount());
        assertEquals(0, response.skippedCount());
    }

    @Test
    void shouldIncludeAnalyzedReviewsWhenForceIsEnabled() {
        ReviewEntity failedReview = failedReview(3L, 10L, 20L, 7);
        ReviewEntity analyzedReview = analyzedReview(4L, 10L, 20L);

        when(reviewRepository.findForReanalysis(
                eq(List.of(ReviewStatus.FAILED_ANALYSIS, ReviewStatus.ANALYZED)),
                eq(10L),
                eq(20L),
                eq(true),
                eq(5),
                any(Pageable.class)
        )).thenReturn(List.of(failedReview, analyzedReview));
        when(reviewRepository.countBySourceIdAndStatus(20L, ReviewStatus.FAILED_ANALYSIS)).thenReturn(1L);

        ReviewReanalysisResponseDto response = reviewReanalysisService.reanalyzeFailedReviews(10L, 20L, 100, true);

        assertEquals(2, response.requestedCount());
        assertEquals(2, response.reanalyzedCount());
        verify(reviewBatchAnalysisService).analyzeReviews(List.of(failedReview, analyzedReview));
    }

    @Test
    void shouldNotMarkAnalyzedReviewsAsFailedWhenForcedReanalysisFails() {
        ReviewEntity failedReview = failedReview(3L, 10L, 20L, 7);
        ReviewEntity analyzedReview = analyzedReview(4L, 10L, 20L);

        when(reviewRepository.findForReanalysis(
                eq(List.of(ReviewStatus.FAILED_ANALYSIS, ReviewStatus.ANALYZED)),
                eq(10L),
                eq(20L),
                eq(true),
                eq(5),
                any(Pageable.class)
        )).thenReturn(List.of(failedReview, analyzedReview));
        org.mockito.Mockito.doThrow(new RuntimeException("analysis-service unavailable"))
                .when(reviewBatchAnalysisService).analyzeReviews(List.of(failedReview, analyzedReview));

        ReviewReanalysisResponseDto response = reviewReanalysisService.reanalyzeFailedReviews(10L, 20L, 100, true);

        assertEquals(2, response.requestedCount());
        assertEquals(2, response.failedCount());
        verify(reviewBatchAnalysisService).markFailedAnalysis(List.of(failedReview), "analysis-service unavailable");
    }

    private ReviewEntity failedReview(Long reviewId, Long organizationId, Long sourceId, int retryCount) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(organizationId);

        SourceEntity source = new SourceEntity();
        source.setId(sourceId);
        source.setOrganization(organization);

        ReviewEntity review = new ReviewEntity();
        review.setId(reviewId);
        review.setSource(source);
        review.setStatus(ReviewStatus.FAILED_ANALYSIS);
        review.setAnalysisRetryCount(retryCount);
        review.setText("Retry review");

        ReviewAnalysisEntity analysis = new ReviewAnalysisEntity();
        analysis.setId(reviewId + 100);
        analysis.setReview(review);
        review.setAnalysis(analysis);

        return review;
    }

    private ReviewEntity analyzedReview(Long reviewId, Long organizationId, Long sourceId) {
        ReviewEntity review = failedReview(reviewId, organizationId, sourceId, 0);
        review.setStatus(ReviewStatus.ANALYZED);
        return review;
    }
}
