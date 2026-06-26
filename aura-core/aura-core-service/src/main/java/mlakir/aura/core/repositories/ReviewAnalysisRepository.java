package mlakir.aura.core.repositories;

import java.util.List;
import mlakir.aura.core.enums.ReviewTopic;
import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.models.ReviewAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewAnalysisRepository extends JpaRepository<ReviewAnalysisEntity, Long> {

    @Query("""
            select ra.sentiment as sentiment, count(ra) as count
            from ReviewAnalysisEntity ra
            where ra.review.source.organization.id = :organizationId
            group by ra.sentiment
            order by count(ra) desc
            """)
    List<SentimentCountProjection> countByOrganizationIdAndSentiment(@Param("organizationId") Long organizationId);

    @Query("""
            select ra.topic as topic, count(ra) as count
            from ReviewAnalysisEntity ra
            where ra.review.source.organization.id = :organizationId
            group by ra.topic
            order by count(ra) desc
            """)
    List<TopicCountProjection> countByOrganizationIdAndTopic(@Param("organizationId") Long organizationId);

    @Query(value = """
            select lower(trim(keyword)) as keyword, count(*) as count
            from aura_core.review_analysis ra
            join aura_core.reviews r on r.id = ra.review_id
            join aura_core.sources s on s.id = r.source_id
            cross join unnest(string_to_array(coalesce(ra.keywords, ''), ',')) as keyword
            where (:organizationId is null or s.organization_id = :organizationId)
              and trim(keyword) <> ''
            group by lower(trim(keyword))
            order by count(*) desc, lower(trim(keyword)) asc
            limit :limit
            """, nativeQuery = true)
    List<KeywordCountProjection> findPopularKeywords(@Param("organizationId") Long organizationId,
                                                     @Param("limit") int limit);

    interface SentimentCountProjection {
        SentimentType getSentiment();

        long getCount();
    }

    interface TopicCountProjection {
        ReviewTopic getTopic();

        long getCount();
    }

    interface KeywordCountProjection {
        String getKeyword();

        long getCount();
    }
}
