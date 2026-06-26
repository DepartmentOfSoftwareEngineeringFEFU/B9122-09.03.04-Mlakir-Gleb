package mlakir.aura.core.specifications;

import mlakir.aura.core.models.OrganizationEntity;
import org.springframework.data.jpa.domain.Specification;

public final class OrganizationSpecifications {

    private OrganizationSpecifications() {
    }

    public static Specification<OrganizationEntity> nameContains(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null) {
                return null;
            }
            String pattern = "%" + name.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("shortName")), pattern)
            );
        };
    }

    public static Specification<OrganizationEntity> isActiveEquals(Boolean isActive) {
        return (root, query, criteriaBuilder) ->
                isActive == null ? null : criteriaBuilder.equal(root.get("isActive"), isActive);
    }

    public static Specification<OrganizationEntity> orderedByName() {
        return (root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.asc(root.get("name")));
            return criteriaBuilder.conjunction();
        };
    }
}
