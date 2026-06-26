package mlakir.aura.core.mappers;

import java.time.OffsetDateTime;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.services.ReviewCandidate;
import org.springframework.stereotype.Component;

@Component
public class ReviewCandidateMapper {

    public ReviewEntity toEntity(SourceEntity source, ReviewCandidate candidate) {
        ReviewEntity review = new ReviewEntity();
        review.setSource(source);
        review.setExternalId(candidate.externalId());
        review.setText(candidate.text());
        review.setAuthorName(candidate.authorName());
        review.setRating(candidate.rating());
        review.setPublishedAt(candidate.publishedAt());
        review.setOriginalUrl(candidate.originalUrl());
        review.setCollectedAt(OffsetDateTime.now());
        review.setStatus(ReviewStatus.ANALYSIS_PENDING);
        review.setAnalysisRetryCount(0);
        review.setLastAnalysisAttemptAt(null);
        review.setAnalysisErrorMessage(null);
        return review;
    }
}
