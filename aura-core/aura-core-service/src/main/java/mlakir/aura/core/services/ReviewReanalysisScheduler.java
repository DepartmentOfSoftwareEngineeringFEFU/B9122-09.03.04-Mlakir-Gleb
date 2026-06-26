package mlakir.aura.core.services;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.dto.ReviewReanalysisResponseDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "reviews.reanalysis", name = "enabled", havingValue = "true")
public class ReviewReanalysisScheduler {

    private final ReviewReanalysisService reviewReanalysisService;
    private final ReviewsReanalysisProperties reviewsReanalysisProperties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${reviews.reanalysis.fixed-delay-ms:300000}")
    public void reanalyzeFailedReviews() {
        if (!running.compareAndSet(false, true)) {
            log.info("Skipping scheduled review reanalysis because previous run is still active");
            return;
        }

        try {
            ReviewReanalysisResponseDto response = reviewReanalysisService.reanalyzeFailedReviews(
                    null,
                    null,
                    reviewsReanalysisProperties.getBatchSize(),
                    false
            );
            log.info("Scheduled review reanalysis finished: requestedCount={}, reanalyzedCount={}, failedCount={}, skippedCount={}, errorMessage={}",
                    response.requestedCount(),
                    response.reanalyzedCount(),
                    response.failedCount(),
                    response.skippedCount(),
                    response.errorMessage());
        } finally {
            running.set(false);
        }
    }
}
