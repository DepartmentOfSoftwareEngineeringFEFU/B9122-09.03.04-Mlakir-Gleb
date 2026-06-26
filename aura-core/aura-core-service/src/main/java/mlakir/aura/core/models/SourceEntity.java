package mlakir.aura.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import mlakir.aura.core.enums.CollectionMode;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.models.AuditableEntity;

@Getter
@Setter
@Entity
@Table(name = "sources", schema = "aura_core")
public class SourceEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private SourceType type;

    @Column(name = "base_url", nullable = false, length = 1000)
    private String baseUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_mode", nullable = false, length = 50)
    private CollectionMode collectionMode;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "schedule_enabled", nullable = false)
    private Boolean scheduleEnabled;

    @Column(name = "schedule_interval_minutes")
    private Integer scheduleIntervalMinutes;

    @Column(name = "last_collected_at")
    private OffsetDateTime lastCollectedAt;

    @Column(name = "next_collection_at")
    private OffsetDateTime nextCollectionAt;
}
