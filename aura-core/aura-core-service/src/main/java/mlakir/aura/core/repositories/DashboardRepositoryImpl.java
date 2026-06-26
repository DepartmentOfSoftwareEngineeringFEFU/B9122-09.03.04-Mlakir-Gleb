package mlakir.aura.core.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import mlakir.aura.core.models.ReviewAnalysisEntity;
import mlakir.aura.core.models.ReviewEntity;
import org.springframework.stereotype.Repository;

@Repository
class DashboardRepositoryImpl implements DashboardRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countReviews(DashboardFilters filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ReviewEntity> review = query.from(ReviewEntity.class);

        query.select(cb.count(review));
        query.where(buildPredicates(filters, cb, review, null));

        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public long countSources(DashboardFilters filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ReviewEntity> review = query.from(ReviewEntity.class);

        query.select(cb.countDistinct(review.get("source").get("id")));
        query.where(buildPredicates(filters, cb, review, null));

        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public List<SentimentCount> aggregateSentiment(DashboardFilters filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<ReviewEntity> review = query.from(ReviewEntity.class);
        Join<ReviewEntity, ReviewAnalysisEntity> analysis = review.join("analysis", JoinType.INNER);

        query.multiselect(
                analysis.get("sentiment").alias("sentiment"),
                cb.count(review).alias("count")
        );
        query.where(buildPredicates(filters, cb, review, analysis));
        query.groupBy(analysis.get("sentiment"));

        return entityManager.createQuery(query).getResultList().stream()
                .map(tuple -> new SentimentCount(
                        tuple.get("sentiment", mlakir.aura.core.enums.SentimentType.class),
                        tuple.get("count", Long.class)
                ))
                .toList();
    }

    @Override
    public List<CategoryCount> topCategories(DashboardFilters filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<ReviewEntity> review = query.from(ReviewEntity.class);
        Join<ReviewEntity, ReviewAnalysisEntity> analysis = review.join("analysis", JoinType.INNER);

        Expression<Long> count = cb.count(review);
        query.multiselect(
                analysis.get("topic").alias("category"),
                count.alias("count")
        );
        query.where(buildPredicates(filters, cb, review, analysis));
        query.groupBy(analysis.get("topic"));
        query.orderBy(cb.desc(count));

        return entityManager.createQuery(query).setMaxResults(10).getResultList().stream()
                .map(tuple -> new CategoryCount(
                        tuple.get("category", mlakir.aura.core.enums.ReviewTopic.class),
                        tuple.get("count", Long.class)
                ))
                .toList();
    }

    @Override
    public List<TimelineCount> timeline(DashboardFilters filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<ReviewEntity> review = query.from(ReviewEntity.class);
        Join<ReviewEntity, ReviewAnalysisEntity> analysis = filters.sentiment() == null
                ? null
                : review.join("analysis", JoinType.INNER);
        Expression<String> month = cb.function("to_char", String.class, review.get("publishedAt"), cb.literal("YYYY-MM"));
        Expression<Long> count = cb.count(review);

        query.multiselect(
                month.alias("month"),
                count.alias("count")
        );
        query.where(buildPredicates(filters, cb, review, analysis));
        query.groupBy(month);
        query.orderBy(cb.asc(month));

        return entityManager.createQuery(query).getResultList().stream()
                .map(tuple -> new TimelineCount(
                        tuple.get("month", String.class),
                        tuple.get("count", Long.class)
                ))
                .toList();
    }

    private Predicate[] buildPredicates(DashboardFilters filters,
                                        CriteriaBuilder cb,
                                        Root<ReviewEntity> review,
                                        Join<ReviewEntity, ReviewAnalysisEntity> analysis) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(review.get("source").get("organization").get("id"), filters.organizationId()));

        if (filters.from() != null) {
            predicates.add(cb.greaterThanOrEqualTo(review.get("publishedAt"), filters.from()));
        }
        if (filters.to() != null) {
            predicates.add(cb.lessThan(review.get("publishedAt"), filters.to()));
        }
        if (filters.sourceId() != null) {
            predicates.add(cb.equal(review.get("source").get("id"), filters.sourceId()));
        }
        if (filters.sentiment() != null) {
            Join<ReviewEntity, ReviewAnalysisEntity> sentimentJoin = analysis == null
                    ? review.join("analysis", JoinType.INNER)
                    : analysis;
            predicates.add(cb.equal(sentimentJoin.get("sentiment"), filters.sentiment()));
        }

        return predicates.toArray(Predicate[]::new);
    }
}
