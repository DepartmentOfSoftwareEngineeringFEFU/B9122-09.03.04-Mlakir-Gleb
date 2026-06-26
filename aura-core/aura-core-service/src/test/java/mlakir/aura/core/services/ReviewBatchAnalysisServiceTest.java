package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.dto.integrations.analysis.AnalyzeResponseDto;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.integrations.analysis.AnalysisIntegrationService;
import mlakir.aura.core.integrations.analysis.AnalysisResultMapper;
import mlakir.aura.core.models.ReviewAnalysisEntity;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.repositories.ReviewAnalysisRepository;
import mlakir.aura.core.repositories.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewBatchAnalysisServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewAnalysisRepository reviewAnalysisRepository;
    @Mock
    private AnalysisIntegrationService analysisIntegrationService;
    @Mock
    private AnalysisResultMapper analysisResultMapper;
    @Mock
    private ReviewAnalysisAttemptService reviewAnalysisAttemptService;

    private ReviewBatchAnalysisService reviewBatchAnalysisService;
    private ReviewAnalysisPersistenceService reviewAnalysisPersistenceService;

    @BeforeEach
    void setUp() {
        reviewAnalysisPersistenceService = new ReviewAnalysisPersistenceService(
                reviewRepository,
                reviewAnalysisRepository,
                analysisResultMapper
        );
        reviewBatchAnalysisService = new ReviewBatchAnalysisService(
                reviewRepository,
                analysisIntegrationService,
                reviewAnalysisAttemptService,
                reviewAnalysisPersistenceService
        );
        org.mockito.Mockito.doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ReviewEntity> reviews = invocation.getArgument(0);
            OffsetDateTime attemptAt = OffsetDateTime.now();
            reviews.forEach(review -> {
                int currentRetryCount = review.getAnalysisRetryCount() == null ? 0 : review.getAnalysisRetryCount();
                review.setAnalysisRetryCount(currentRetryCount + 1);
                review.setLastAnalysisAttemptAt(attemptAt);
                review.setStatus(ReviewStatus.ANALYSIS_PENDING);
                review.setAnalysisErrorMessage(null);
            });
            return null;
        }).when(reviewAnalysisAttemptService).registerAttempt(any());
    }

    @Test
    void shouldAnalyzeReviewsAndPersistAnalysis() {
        ReviewEntity review = new ReviewEntity();
        review.setId(42L);
        review.setText("Great teachers");
        review.setStatus(ReviewStatus.ANALYSIS_PENDING);
        review.setAnalysisRetryCount(0);

        ReviewAnalysisEntity analysis = new ReviewAnalysisEntity();
        analysis.setReview(review);

        when(analysisIntegrationService.analyzeBatch(List.of("Great teachers"), List.of(42L))).thenReturn(List.of(
                new AnalyzeResponseDto("POSITIVE", "TEACHERS", List.of("teachers"), new BigDecimal("0.95"), "v1")
        ));
        when(analysisResultMapper.toEntity(any(ReviewEntity.class), any(AnalyzeResponseDto.class))).thenReturn(analysis);

        reviewBatchAnalysisService.analyzeReviews(List.of(review));

        assertEquals(ReviewStatus.ANALYZED, review.getStatus());
        assertEquals(1, review.getAnalysisRetryCount());
        assertSame(analysis, review.getAnalysis());
        verify(reviewAnalysisRepository).save(analysis);
        verify(reviewAnalysisAttemptService).registerAttempt(List.of(review));
        verify(reviewRepository).saveAll(List.of(review));
    }

    @Test
    void shouldKeepFullReviewTextWhileSendingReviewIdsForAnalysisPreparation() {
        String fullText = "x".repeat(12050);
        ReviewEntity review = new ReviewEntity();
        review.setId(77L);
        review.setText(fullText);
        review.setStatus(ReviewStatus.ANALYSIS_PENDING);
        review.setAnalysisRetryCount(2);

        ReviewAnalysisEntity analysis = new ReviewAnalysisEntity();
        analysis.setReview(review);

        when(analysisIntegrationService.analyzeBatch(List.of(fullText), List.of(77L))).thenReturn(List.of(
                new AnalyzeResponseDto("POSITIVE", "TEACHERS", List.of("teachers"), new BigDecimal("0.95"), "v1")
        ));
        when(analysisResultMapper.toEntity(any(ReviewEntity.class), any(AnalyzeResponseDto.class))).thenReturn(analysis);

        reviewBatchAnalysisService.analyzeReviews(List.of(review));

        assertEquals(12050, review.getText().length());
        assertEquals(3, review.getAnalysisRetryCount());
        verify(analysisIntegrationService).analyzeBatch(List.of(fullText), List.of(77L));
        verify(reviewAnalysisAttemptService).registerAttempt(List.of(review));
    }

    @Test
    void shouldUpdateExistingAnalysisInsteadOfCreatingDuplicate() {
        ReviewEntity review = new ReviewEntity();
        review.setId(55L);
        review.setText("Dormitory review");
        review.setStatus(ReviewStatus.FAILED_ANALYSIS);
        review.setAnalysisRetryCount(1);

        ReviewAnalysisEntity existingAnalysis = new ReviewAnalysisEntity();
        existingAnalysis.setId(9L);
        existingAnalysis.setReview(review);
        review.setAnalysis(existingAnalysis);

        when(analysisIntegrationService.analyzeBatch(List.of("Dormitory review"), List.of(55L))).thenReturn(List.of(
                new AnalyzeResponseDto("NEGATIVE", "DORMITORY", List.of("dormitory"), new BigDecimal("0.81"), "v2")
        ));

        reviewBatchAnalysisService.analyzeReviews(List.of(review));

        assertEquals(ReviewStatus.ANALYZED, review.getStatus());
        assertEquals(2, review.getAnalysisRetryCount());
        verify(analysisResultMapper).updateEntity(existingAnalysis, new AnalyzeResponseDto(
                "NEGATIVE", "DORMITORY", List.of("dormitory"), new BigDecimal("0.81"), "v2"
        ));
        verify(reviewAnalysisRepository).save(existingAnalysis);
        verify(reviewAnalysisAttemptService).registerAttempt(List.of(review));
        verify(reviewRepository).saveAll(List.of(review));
    }
}
