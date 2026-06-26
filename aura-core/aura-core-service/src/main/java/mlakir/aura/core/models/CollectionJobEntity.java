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
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import mlakir.aura.core.enums.CollectionJobStatus;
import mlakir.aura.core.models.SourceEntity;

@Getter
@Setter
@Entity
@Table(name = "collection_jobs", schema = "aura_core")
public class CollectionJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private SourceEntity source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private CollectionJobStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "collected_count", nullable = false)
    private Integer collectedCount;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "triggered_by", nullable = false, length = 255)
    private String triggeredBy;
}
