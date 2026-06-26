package mlakir.aura.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "organizations", schema = "aura_core")
public class OrganizationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255, unique = true)
    private String name;

    @Column(name = "short_name", nullable = false, length = 100, unique = true)
    private String shortName;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "website", length = 1000)
    private String website;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
