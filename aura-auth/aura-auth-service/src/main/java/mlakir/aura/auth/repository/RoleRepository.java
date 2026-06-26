package mlakir.aura.auth.repository;

import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.*;

import mlakir.aura.auth.entity.*;

public interface RoleRepository extends JpaRepository<RoleEntity, Long>, PagingAndSortingRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByCode(String code);

}
