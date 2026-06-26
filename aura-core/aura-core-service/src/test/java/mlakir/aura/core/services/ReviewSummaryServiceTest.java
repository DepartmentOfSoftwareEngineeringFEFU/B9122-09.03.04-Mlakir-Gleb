package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import mlakir.aura.core.dto.ReviewSummaryResponseDto;
import mlakir.aura.core.dto.integrations.analysis.SummarizeResponseDto;
import mlakir.aura.core.exceptions.ReviewExceptionFactory;
import mlakir.aura.core.integrations.analysis.AnalysisIntegrationException;
import mlakir.aura.core.integrations.analysis.AnalysisIntegrationService;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.repositories.ReviewRepository;
import mlakir.aura.exception.AuraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewSummaryServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private AnalysisIntegrationService analysisIntegrationService;

    private ReviewsSummaryProperties reviewsSummaryProperties;
    private ReviewSummaryService reviewSummaryService;

    @BeforeEach
    void setUp() {
        reviewsSummaryProperties = new ReviewsSummaryProperties();
        reviewsSummaryProperties.setMaxInputLength(20000);
        reviewSummaryService = new ReviewSummaryService(
                reviewRepository,
                analysisIntegrationService,
                new ReviewExceptionFactory(),
                reviewsSummaryProperties
        );
    }

    @Test
    void shouldReturnCachedSummaryWhenItAlreadyExists() {
        ReviewEntity review = review("Существующий summary", "model-v1");
        when(reviewRepository.findById(1L)).thenReturn(java.util.Optional.of(review));

        ReviewSummaryResponseDto response = reviewSummaryService.getOrGenerateSummary(1L, false);

        assertEquals(1L, response.reviewId());
        assertEquals("Существующий summary", response.summary());
        assertEquals("model-v1", response.modelVersion());
        assertEquals(true, response.cached());
        verify(analysisIntegrationService, never()).summarize(anyString());
        verify(reviewRepository, never()).save(review);
    }

    @Test
    void shouldGenerateSummaryWhenItDoesNotExist() {
        ReviewEntity review = review(null, null);
        when(reviewRepository.findById(1L)).thenReturn(java.util.Optional.of(review));
        when(analysisIntegrationService.summarize("Очень длинный отзыв")).thenReturn(
                new SummarizeResponseDto("Краткий конспект", "deepseek-openrouter-0.1.0")
        );
        when(reviewRepository.save(review)).thenReturn(review);

        ReviewSummaryResponseDto response = reviewSummaryService.getOrGenerateSummary(1L, false);

        assertEquals("Краткий конспект", response.summary());
        assertEquals("deepseek-openrouter-0.1.0", response.modelVersion());
        assertEquals(false, response.cached());
        assertEquals("Краткий конспект", review.getSummary());
        assertEquals("deepseek-openrouter-0.1.0", review.getSummaryModelVersion());
        verify(analysisIntegrationService).summarize("Очень длинный отзыв");
        verify(reviewRepository).save(review);
    }

    @Test
    void shouldReturnCachedTrueOnRepeatedRequest() {
        ReviewEntity review = review("Уже есть summary", "model-v1");
        when(reviewRepository.findById(1L)).thenReturn(java.util.Optional.of(review));

        ReviewSummaryResponseDto response = reviewSummaryService.getOrGenerateSummary(1L, false);

        assertEquals(true, response.cached());
        verify(analysisIntegrationService, never()).summarize(anyString());
    }

    @Test
    void shouldForceSummaryRegeneration() {
        ReviewEntity review = review("Старый summary", "model-v1");
        when(reviewRepository.findById(1L)).thenReturn(java.util.Optional.of(review));
        when(analysisIntegrationService.summarize("Очень длинный отзыв")).thenReturn(
                new SummarizeResponseDto("Новый summary", "model-v2")
        );
        when(reviewRepository.save(review)).thenReturn(review);

        ReviewSummaryResponseDto response = reviewSummaryService.getOrGenerateSummary(1L, true);

        assertEquals("Новый summary", response.summary());
        assertEquals("model-v2", response.modelVersion());
        assertEquals(false, response.cached());
        verify(analysisIntegrationService).summarize("Очень длинный отзыв");
        verify(reviewRepository).save(review);
    }

    @Test
    void shouldTruncateSummaryInputBeforeSendingToAnalysisService() {
        reviewsSummaryProperties.setMaxInputLength(5);
        ReviewEntity review = review(null, null);
        review.setText("  123456789  ");
        when(reviewRepository.findById(1L)).thenReturn(java.util.Optional.of(review));
        when(analysisIntegrationService.summarize("12345")).thenReturn(
                new SummarizeResponseDto("Кратко", "model-v1")
        );
        when(reviewRepository.save(review)).thenReturn(review);

        reviewSummaryService.getOrGenerateSummary(1L, false);

        verify(analysisIntegrationService).summarize("12345");
    }

    @Test
    void shouldThrowNotFoundWhenReviewDoesNotExist() {
        when(reviewRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(AuraException.class, () -> reviewSummaryService.getOrGenerateSummary(99L, false));
        verify(analysisIntegrationService, never()).summarize(anyString());
    }

    @Test
    void shouldThrowMeaningfulErrorWhenAnalysisServiceFails() {
        ReviewEntity review = review(null, null);
        when(reviewRepository.findById(1L)).thenReturn(java.util.Optional.of(review));
        when(analysisIntegrationService.summarize("Очень длинный отзыв"))
                .thenThrow(new AnalysisIntegrationException("analysis-service is unavailable"));

        AuraException exception = assertThrows(
                AuraException.class,
                () -> reviewSummaryService.getOrGenerateSummary(1L, false)
        );

        assertEquals("Summary generation unavailable", exception.getBody().getTitle());
        assertEquals(null, review.getSummary());
        verify(reviewRepository, never()).save(review);
    }

    private ReviewEntity review(String summary, String modelVersion) {
        ReviewEntity review = new ReviewEntity();
        review.setId(1L);
        review.setText("Очень длинный отзыв");
        review.setSummary(summary);
        review.setSummaryGeneratedAt(OffsetDateTime.parse("2026-04-27T12:00:00Z"));
        review.setSummaryModelVersion(modelVersion);
        return review;
    }
}
