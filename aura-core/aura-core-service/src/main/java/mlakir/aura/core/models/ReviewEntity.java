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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.models.SourceEntity;

@Getter
@Setter
@Entity
@Table(name = "reviews", schema = "aura_core")
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private SourceEntity source;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(name = "text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "published_at", nullable = false)
    private OffsetDateTime publishedAt;

    @Column(name = "original_url", length = 1000)
    private String originalUrl;

    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReviewStatus status;

    @Column(name = "analysis_retry_count", nullable = false)
    private Integer analysisRetryCount;

    @Column(name = "last_analysis_attempt_at")
    private OffsetDateTime lastAnalysisAttemptAt;

    @Column(name = "analysis_error_message", columnDefinition = "text")
    private String analysisErrorMessage;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "summary_generated_at")
    private OffsetDateTime summaryGeneratedAt;

    @Column(name = "summary_model_version", length = 100)
    private String summaryModelVersion;

    @OneToOne(mappedBy = "review", fetch = FetchType.LAZY)
    private ReviewAnalysisEntity analysis;
}
