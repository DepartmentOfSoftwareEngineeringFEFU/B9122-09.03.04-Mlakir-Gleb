package mlakir.aura.core.integrations.analysis;

import java.time.OffsetDateTime;
import mlakir.aura.core.enums.ReviewTopic;
import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.dto.integrations.analysis.AnalyzeResponseDto;
import mlakir.aura.core.models.ReviewAnalysisEntity;
import mlakir.aura.core.models.ReviewEntity;
import org.springframework.stereotype.Component;

@Component
public class AnalysisResultMapper {

    public ReviewAnalysisEntity toEntity(ReviewEntity review, AnalyzeResponseDto response) {
        ReviewAnalysisEntity analysis = new ReviewAnalysisEntity();
        analysis.setReview(review);
        apply(analysis, response);
        return analysis;
    }

    public void updateEntity(ReviewAnalysisEntity analysis, AnalyzeResponseDto response) {
        apply(analysis, response);
    }

    private SentimentType parseSentiment(String value) {
        try {
            return SentimentType.valueOf(value);
        } catch (Exception exception) {
            throw new AnalysisIntegrationException("Unknown sentiment from analysis-service: " + value, exception);
        }
    }

    private ReviewTopic parseTopic(String value) {
        try {
            return ReviewTopic.valueOf(value);
        } catch (Exception exception) {
            throw new AnalysisIntegrationException("Unknown topic from analysis-service: " + value, exception);
        }
    }

    private void apply(ReviewAnalysisEntity analysis, AnalyzeResponseDto response) {
        analysis.setSentiment(parseSentiment(response.sentiment()));
        analysis.setTopic(parseTopic(response.topic()));
        analysis.setKeywords(String.join(", ", response.keywords()));
        analysis.setConfidence(response.confidence());
        analysis.setModelVersion(response.modelVersion());
        analysis.setAnalyzedAt(OffsetDateTime.now());
    }
}
