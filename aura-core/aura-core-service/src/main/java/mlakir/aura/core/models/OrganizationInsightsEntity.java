package mlakir.aura.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "organization_insights", schema = "aura_core")
public class OrganizationInsightsEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(name = "summary", nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "strengths", nullable = false, columnDefinition = "text")
    private String strengths;

    @Column(name = "weaknesses", nullable = false, columnDefinition = "text")
    private String weaknesses;

    @Column(name = "recommendations", nullable = false, columnDefinition = "text")
    private String recommendations;

    @Column(name = "reviews_used", nullable = false)
    private Integer reviewsUsed;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;
}
