package mlakir.aura.core.integrations.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.enums.ReviewTopic;
import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.dto.integrations.analysis.AnalyzeResponseDto;
import mlakir.aura.core.models.ReviewEntity;
import org.junit.jupiter.api.Test;

class AnalysisResultMapperTest {

    private final AnalysisResultMapper mapper = new AnalysisResultMapper();

    @Test
    void shouldMapAnalyzeResponseToDomainEntity() {
        ReviewEntity review = new ReviewEntity();
        review.setExternalId("review-1");
        review.setText("Очень хорошие преподаватели");
        review.setCollectedAt(OffsetDateTime.now());
        review.setPublishedAt(OffsetDateTime.now());
        review.setStatus(ReviewStatus.ANALYSIS_PENDING);

        AnalyzeResponseDto response = new AnalyzeResponseDto(
                "POSITIVE",
                "TEACHERS",
                List.of("преподаватели"),
                new BigDecimal("0.93"),
                "rule-based-v1"
        );

        var analysis = mapper.toEntity(review, response);

        assertEquals(SentimentType.POSITIVE, analysis.getSentiment());
        assertEquals(ReviewTopic.TEACHERS, analysis.getTopic());
        assertEquals("преподаватели", analysis.getKeywords());
        assertEquals("rule-based-v1", analysis.getModelVersion());
    }

    @Test
    void shouldFailOnUnknownTopic() {
        ReviewEntity review = new ReviewEntity();
        review.setExternalId("review-2");

        AnalyzeResponseDto response = new AnalyzeResponseDto(
                "POSITIVE",
                "UNKNOWN_TOPIC",
                List.of("test"),
                new BigDecimal("0.50"),
                "rule-based-v1"
        );

        assertThrows(AnalysisIntegrationException.class, () -> mapper.toEntity(review, response));
    }
}
