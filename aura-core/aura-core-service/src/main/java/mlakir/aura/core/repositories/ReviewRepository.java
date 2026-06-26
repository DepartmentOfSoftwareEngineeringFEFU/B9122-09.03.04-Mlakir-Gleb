package mlakir.aura.core.repositories;

import java.util.Optional;
import java.util.List;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.models.ReviewEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long>, JpaSpecificationExecutor<ReviewEntity> {

    boolean existsBySourceIdAndExternalId(Long sourceId, String externalId);

    long countBySourceIdAndStatus(Long sourceId, ReviewStatus status);

    @EntityGraph(attributePaths = {"source", "analysis"})
    @Query("select r from ReviewEntity r where r.id = :id")
    Optional<ReviewEntity> findDetailedById(@Param("id") Long id);

    @Query("""
            select r from ReviewEntity r
            join fetch r.source s
            left join fetch r.analysis a
            where r.status in :statuses
              and (:organizationId is null or s.organization.id = :organizationId)
              and (:sourceId is null or s.id = :sourceId)
              and (:force = true or coalesce(r.analysisRetryCount, 0) < :maxRetries)
            order by coalesce(r.lastAnalysisAttemptAt, r.collectedAt) asc, r.id asc
            """)
    List<ReviewEntity> findForReanalysis(@Param("statuses") List<ReviewStatus> statuses,
                                         @Param("organizationId") Long organizationId,
                                         @Param("sourceId") Long sourceId,
                                         @Param("force") boolean force,
                                         @Param("maxRetries") int maxRetries,
                                         Pageable pageable);

    @Query("""
            select count(r) from ReviewEntity r
            join r.source s
            where r.status = :status
              and (:organizationId is null or s.organization.id = :organizationId)
              and (:sourceId is null or s.id = :sourceId)
              and coalesce(r.analysisRetryCount, 0) >= :maxRetries
            """)
    long countSkippedForReanalysis(@Param("status") ReviewStatus status,
                                   @Param("organizationId") Long organizationId,
                                   @Param("sourceId") Long sourceId,
                                   @Param("maxRetries") int maxRetries);

}
