package mlakir.aura.core.services;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.dto.ReviewSummaryResponseDto;
import mlakir.aura.core.dto.integrations.analysis.SummarizeResponseDto;
import mlakir.aura.core.exceptions.ReviewExceptionFactory;
import mlakir.aura.core.integrations.analysis.AnalysisIntegrationException;
import mlakir.aura.core.integrations.analysis.AnalysisIntegrationService;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.repositories.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSummaryService {

    private final ReviewRepository reviewRepository;
    private final AnalysisIntegrationService analysisIntegrationService;
    private final ReviewExceptionFactory reviewExceptionFactory;
    private final ReviewsSummaryProperties reviewsSummaryProperties;

    @Transactional
    public ReviewSummaryResponseDto getOrGenerateSummary(Long reviewId, boolean force) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> reviewExceptionFactory.reviewNotFound(reviewId));

        if (!force && hasSummary(review)) {
            return toResponse(review, true);
        }

        String preparedText = prepareTextForSummary(reviewId, review.getText());

        try {
            SummarizeResponseDto summaryResponse = analysisIntegrationService.summarize(preparedText);
            review.setSummary(summaryResponse.summary());
            review.setSummaryGeneratedAt(OffsetDateTime.now());
            review.setSummaryModelVersion(summaryResponse.modelVersion());
            ReviewEntity saved = reviewRepository.save(review);
            return toResponse(saved, false);
        } catch (AnalysisIntegrationException exception) {
            log.warn("Summary generation failed for reviewId={}: {}", reviewId, exception.getMessage());
            throw reviewExceptionFactory.summaryGenerationUnavailable();
        }
    }

    private ReviewSummaryResponseDto toResponse(ReviewEntity review, boolean cached) {
        return new ReviewSummaryResponseDto(
                review.getId(),
                review.getSummary(),
                review.getSummaryGeneratedAt(),
                review.getSummaryModelVersion(),
                cached
        );
    }

    private boolean hasSummary(ReviewEntity review) {
        return review.getSummary() != null && !review.getSummary().isBlank();
    }

    private String prepareTextForSummary(Long reviewId, String text) {
        String prepared = text == null ? "" : text.trim();
        int maxInputLength = Math.max(reviewsSummaryProperties.getMaxInputLength(), 1);
        if (prepared.length() <= maxInputLength) {
            return prepared;
        }

        log.info("Review text truncated for summary generation: reviewId={}, originalLength={}, maxLength={}",
                reviewId, prepared.length(), maxInputLength);
        return prepared.substring(0, maxInputLength);
    }
}
