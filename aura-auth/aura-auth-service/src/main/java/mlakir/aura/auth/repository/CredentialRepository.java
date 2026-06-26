package mlakir.aura.auth.repository;

import java.util.*;

import org.springframework.data.jpa.repository.*;

import mlakir.aura.auth.entity.*;
import org.springframework.data.repository.query.*;

public interface CredentialRepository extends JpaRepository<CredentialEntity, Long> {

    @Query("""
        select c.salt
        from credentials c
        where c.user.id = :userId
        """)
    Optional<String> findSaltByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndHash(Long userId, String hash);

}
