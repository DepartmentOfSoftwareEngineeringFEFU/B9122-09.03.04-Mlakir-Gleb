package mlakir.aura.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import mlakir.aura.core.enums.ReviewTopic;
import mlakir.aura.core.enums.SentimentType;

@Getter
@Setter
@Entity
@Table(name = "review_analysis", schema = "aura_core")
public class ReviewAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private ReviewEntity review;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", nullable = false, length = 50)
    private SentimentType sentiment;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic", nullable = false, length = 50)
    private ReviewTopic topic;

    @Column(name = "keywords", length = 1000)
    private String keywords;

    @Column(name = "confidence", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    @Column(name = "analyzed_at", nullable = false)
    private OffsetDateTime analyzedAt;
}
