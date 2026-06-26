package mlakir.aura.core.repositories;

import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.models.ReviewEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface DashboardRepository extends Repository<ReviewEntity, Long>, DashboardRepositoryCustom {

    @Query("""
            select count(r) from ReviewEntity r
            where r.source.organization.id = :organizationId
            """)
    long countTotalReviews(@Param("organizationId") Long organizationId);

    @Query("""
            select count(ra) from ReviewAnalysisEntity ra
            where ra.review.source.organization.id = :organizationId
              and ra.sentiment = :sentiment
            """)
    long countBySentiment(@Param("organizationId") Long organizationId,
                          @Param("sentiment") SentimentType sentiment);
}
