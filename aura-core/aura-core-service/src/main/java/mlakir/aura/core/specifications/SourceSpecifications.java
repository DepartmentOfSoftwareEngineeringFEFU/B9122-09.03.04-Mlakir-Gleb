package mlakir.aura.core.specifications;

import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.models.SourceEntity;
import org.springframework.data.jpa.domain.Specification;

public final class SourceSpecifications {

    private SourceSpecifications() {
    }

    public static Specification<SourceEntity> organizationIdEquals(Long organizationId) {
        return (root, query, criteriaBuilder) ->
                organizationId == null ? null : criteriaBuilder.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<SourceEntity> nameContains(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null) {
                return null;
            }
            String pattern = "%" + name.toLowerCase() + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern);
        };
    }

    public static Specification<SourceEntity> typeEquals(SourceType type) {
        return (root, query, criteriaBuilder) ->
                type == null ? null : criteriaBuilder.equal(root.get("type"), type);
    }

    public static Specification<SourceEntity> isActiveEquals(Boolean isActive) {
        return (root, query, criteriaBuilder) ->
                isActive == null ? null : criteriaBuilder.equal(root.get("isActive"), isActive);
    }

    public static Specification<SourceEntity> scheduleEnabledEquals(Boolean scheduleEnabled) {
        return (root, query, criteriaBuilder) ->
                scheduleEnabled == null ? null : criteriaBuilder.equal(root.get("scheduleEnabled"), scheduleEnabled);
    }

    public static Specification<SourceEntity> withOrganizationFetch() {
        return (root, query, criteriaBuilder) -> {
            if (SourceEntity.class.equals(query.getResultType())) {
                root.fetch("organization");
                query.distinct(true);
                query.orderBy(criteriaBuilder.asc(root.get("name")));
            }
            return criteriaBuilder.conjunction();
        };
    }
}
