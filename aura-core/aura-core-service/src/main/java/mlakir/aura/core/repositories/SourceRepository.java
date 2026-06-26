package mlakir.aura.core.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.models.SourceEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SourceRepository extends JpaRepository<SourceEntity, Long>, JpaSpecificationExecutor<SourceEntity> {

    boolean existsByOrganizationIdAndNameIgnoreCaseAndType(Long organizationId, String name, SourceType type);

    @EntityGraph(attributePaths = "organization")
    List<SourceEntity> findAll();

    @EntityGraph(attributePaths = "organization")
    Optional<SourceEntity> findById(Long id);

    Optional<SourceEntity> findByIdAndIsActiveTrue(Long id);

    @EntityGraph(attributePaths = "organization")
    List<SourceEntity> findByScheduleEnabledTrueAndNextCollectionAtLessThanEqual(OffsetDateTime now);
}
