package mlakir.aura.core.services;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.dto.integrations.analysis.AnalyzeResponseDto;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.integrations.analysis.AnalysisResultMapper;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.repositories.ReviewAnalysisRepository;
import mlakir.aura.core.repositories.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewAnalysisPersistenceService {

    private final ReviewRepository reviewRepository;
    private final ReviewAnalysisRepository reviewAnalysisRepository;
    private final AnalysisResultMapper analysisResultMapper;

    @Transactional
    public void saveSuccessfulAnalysis(List<ReviewEntity> reviews, List<AnalyzeResponseDto> analysisResults) {
        List<ReviewEntity> analyzedReviews = new ArrayList<>(reviews.size());
        for (int index = 0; index < reviews.size(); index++) {
            ReviewEntity review = reviews.get(index);
            var analysis = review.getAnalysis();
            if (analysis == null) {
                analysis = analysisResultMapper.toEntity(review, analysisResults.get(index));
            } else {
                analysisResultMapper.updateEntity(analysis, analysisResults.get(index));
            }
            reviewAnalysisRepository.save(analysis);
            review.setAnalysis(analysis);
            review.setStatus(ReviewStatus.ANALYZED);
            review.setAnalysisErrorMessage(null);
            analyzedReviews.add(review);
        }
        reviewRepository.saveAll(analyzedReviews);
    }
}
