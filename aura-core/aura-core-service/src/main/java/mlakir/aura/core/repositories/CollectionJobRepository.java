package mlakir.aura.core.repositories;

import java.util.Optional;
import mlakir.aura.core.enums.CollectionJobStatus;
import mlakir.aura.core.models.CollectionJobEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CollectionJobRepository extends JpaRepository<CollectionJobEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "source")
    java.util.List<CollectionJobEntity> findAll();

    @EntityGraph(attributePaths = "source")
    @Query("select cj from CollectionJobEntity cj where cj.id = :id")
    Optional<CollectionJobEntity> findDetailedById(Long id);

    boolean existsBySourceIdAndStatus(Long sourceId, CollectionJobStatus status);

    Optional<CollectionJobEntity> findFirstBySourceIdAndStatusOrderByStartedAtDescIdDesc(
            Long sourceId,
            CollectionJobStatus status
    );
}
