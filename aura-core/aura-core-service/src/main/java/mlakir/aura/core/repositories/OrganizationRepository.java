package mlakir.aura.core.repositories;

import mlakir.aura.core.models.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long>,
        JpaSpecificationExecutor<OrganizationEntity> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByShortNameIgnoreCase(String shortName);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByShortNameIgnoreCaseAndIdNot(String shortName, Long id);
}
