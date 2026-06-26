package mlakir.aura.core.specifications;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import mlakir.aura.core.enums.ReviewTopic;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.models.ReviewEntity;
import org.springframework.data.jpa.domain.Specification;

public final class ReviewSpecifications {

    private ReviewSpecifications() {
    }

    public static Specification<ReviewEntity> organizationIdEquals(Long organizationId) {
        return (root, query, criteriaBuilder) ->
                organizationId == null
                        ? null
                        : criteriaBuilder.equal(root.get("source").get("organization").get("id"), organizationId);
    }

    public static Specification<ReviewEntity> sourceIdEquals(Long sourceId) {
        return (root, query, criteriaBuilder) ->
                sourceId == null ? null : criteriaBuilder.equal(root.get("source").get("id"), sourceId);
    }

    public static Specification<ReviewEntity> hasSentiment(SentimentType sentiment) {
        return (root, query, criteriaBuilder) -> {
            if (sentiment == null) {
                return null;
            }
            var analysisJoin = analysisJoin(root);
            return criteriaBuilder.equal(analysisJoin.get("sentiment"), sentiment);
        };
    }

    public static Specification<ReviewEntity> hasTopic(ReviewTopic topic) {
        return (root, query, criteriaBuilder) -> {
            if (topic == null) {
                return null;
            }
            var analysisJoin = analysisJoin(root);
            return criteriaBuilder.equal(analysisJoin.get("topic"), topic);
        };
    }

    public static Specification<ReviewEntity> keywordContains(String keyword) {
        return (root, query, criteriaBuilder) -> {
            String normalized = normalize(keyword);
            if (normalized == null) {
                return null;
            }
            var analysisJoin = analysisJoin(root);
            return criteriaBuilder.like(
                    criteriaBuilder.lower(analysisJoin.get("keywords").as(String.class)),
                    "%" + normalized.toLowerCase() + "%"
            );
        };
    }

    public static Specification<ReviewEntity> statusEquals(ReviewStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<ReviewEntity> publishedAtFrom(LocalDate dateFrom) {
        return (root, query, criteriaBuilder) -> {
            if (dateFrom == null) {
                return null;
            }
            OffsetDateTime boundary = dateFrom.atStartOfDay().atOffset(ZoneOffset.UTC);
            return criteriaBuilder.greaterThanOrEqualTo(root.get("publishedAt"), boundary);
        };
    }

    public static Specification<ReviewEntity> publishedAtTo(LocalDate dateTo) {
        return (root, query, criteriaBuilder) -> {
            if (dateTo == null) {
                return null;
            }
            OffsetDateTime boundary = dateTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            return criteriaBuilder.lessThan(root.get("publishedAt"), boundary);
        };
    }

    public static Specification<ReviewEntity> publishedAtFrom(OffsetDateTime dateFrom) {
        return (root, query, criteriaBuilder) ->
                dateFrom == null ? null : criteriaBuilder.greaterThanOrEqualTo(root.get("publishedAt"), dateFrom);
    }

    public static Specification<ReviewEntity> publishedAtToExclusive(OffsetDateTime dateTo) {
        return (root, query, criteriaBuilder) ->
                dateTo == null ? null : criteriaBuilder.lessThan(root.get("publishedAt"), dateTo);
    }

    public static Specification<ReviewEntity> withFetches() {
        return (root, query, criteriaBuilder) -> {
            if (ReviewEntity.class.equals(query.getResultType())) {
                root.fetch("source", JoinType.LEFT);
                root.fetch("analysis", JoinType.LEFT);
                query.distinct(true);
            }
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<ReviewEntity> orderedByPublishedAtDesc() {
        return (root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.desc(root.get("publishedAt")), criteriaBuilder.desc(root.get("id")));
            return criteriaBuilder.conjunction();
        };
    }

    @SuppressWarnings("unchecked")
    private static Join<ReviewEntity, ?> analysisJoin(From<?, ReviewEntity> root) {
        for (Fetch<ReviewEntity, ?> fetch : root.getFetches()) {
            if ("analysis".equals(fetch.getAttribute().getName()) && fetch instanceof Join<?, ?> join) {
                return (Join<ReviewEntity, ?>) join;
            }
        }

        for (Join<ReviewEntity, ?> join : root.getJoins()) {
            if ("analysis".equals(join.getAttribute().getName())) {
                return join;
            }
        }

        return root.join("analysis", JoinType.LEFT);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
