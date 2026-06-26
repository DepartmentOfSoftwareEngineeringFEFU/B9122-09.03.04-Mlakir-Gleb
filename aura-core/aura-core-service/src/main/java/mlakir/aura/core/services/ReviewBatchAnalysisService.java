package mlakir.aura.core.services;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.integrations.analysis.AnalysisIntegrationService;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.repositories.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewBatchAnalysisService {

    private final ReviewRepository reviewRepository;
    private final AnalysisIntegrationService analysisIntegrationService;
    private final ReviewAnalysisAttemptService reviewAnalysisAttemptService;
    private final ReviewAnalysisPersistenceService reviewAnalysisPersistenceService;

    public void analyzeReviews(List<ReviewEntity> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return;
        }

        reviewAnalysisAttemptService.registerAttempt(reviews);

        log.info("Starting external analysis for {} reviews", reviews.size());
        List<String> texts = reviews.stream()
                .map(ReviewEntity::getText)
                .toList();
        List<Long> reviewIds = reviews.stream()
                .map(ReviewEntity::getId)
                .toList();
        var analysisResults = analysisIntegrationService.analyzeBatch(texts, reviewIds);

        reviewAnalysisPersistenceService.saveSuccessfulAnalysis(reviews, analysisResults);
        log.info("External analysis completed successfully for {} reviews", reviews.size());
    }

    @Transactional
    public void markFailedAnalysis(List<ReviewEntity> reviews) {
        markFailedAnalysis(reviews, null);
    }

    @Transactional
    public void markFailedAnalysis(List<ReviewEntity> reviews, String errorMessage) {
        if (reviews == null || reviews.isEmpty()) {
            return;
        }
        log.warn("Marking {} reviews as FAILED_ANALYSIS after analysis-service error", reviews.size());
        reviews.forEach(review -> {
            review.setStatus(ReviewStatus.FAILED_ANALYSIS);
            review.setAnalysisErrorMessage(truncateErrorMessage(errorMessage));
        });
        reviewRepository.saveAll(reviews);
    }

    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= 2000) {
            return errorMessage;
        }
        return errorMessage.substring(0, 2000);
    }
}
