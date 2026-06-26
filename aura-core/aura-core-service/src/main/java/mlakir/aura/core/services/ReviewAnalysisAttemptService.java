package mlakir.aura.core.services;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.repositories.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewAnalysisAttemptService {

    private final ReviewRepository reviewRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerAttempt(List<ReviewEntity> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return;
        }

        OffsetDateTime attemptAt = OffsetDateTime.now();
        reviews.forEach(review -> {
            int currentRetryCount = review.getAnalysisRetryCount() == null ? 0 : review.getAnalysisRetryCount();
            review.setAnalysisRetryCount(currentRetryCount + 1);
            review.setLastAnalysisAttemptAt(attemptAt);
            review.setStatus(ReviewStatus.ANALYSIS_PENDING);
            review.setAnalysisErrorMessage(null);
        });
        reviewRepository.saveAll(reviews);
    }
}
