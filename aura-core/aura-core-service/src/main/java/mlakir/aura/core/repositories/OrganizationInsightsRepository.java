package mlakir.aura.core.repositories;

import java.util.Optional;
import mlakir.aura.core.models.OrganizationInsightsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationInsightsRepository extends JpaRepository<OrganizationInsightsEntity, Long> {

    Optional<OrganizationInsightsEntity> findTopByOrganizationIdOrderByGeneratedAtDescIdDesc(Long organizationId);
}
