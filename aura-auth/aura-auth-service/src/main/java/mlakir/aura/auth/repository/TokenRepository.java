package mlakir.aura.auth.repository;

import java.time.*;
import java.util.*;

import org.springframework.data.jpa.repository.*;

import mlakir.aura.auth.entity.*;
import org.springframework.data.repository.query.*;

public interface TokenRepository extends JpaRepository<TokenEntity, Long> {

    @Query(
        """
                select t
                from tokens t
                where t.accessJti = :jti
                    and t.expiredAt > :now
            """)
    Optional<TokenEntity> findByAccessJti(
        @Param("jti") UUID jti,
        @Param("now") Instant now
    );

    @Query(
        """
                select t
                from tokens t
                where t.refreshJti = :jti
                    and t.expiredAt > :now
            """)
    Optional<TokenEntity> findByRefreshJti(
        @Param("jti") UUID jti,
        @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
                update tokens t
                set t.expiredAt = :now
                where t.refreshJti = :jti
            """)
    void softDeleteByRefreshJti(@Param("jti") UUID jti, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
                update tokens t
                set t.expiredAt = :now
                where t.accessJti = :jti
            """)
    void softDeleteByAccessJti(@Param("jti") UUID jti, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
                update tokens t
                set t.expiredAt = :now
                where t.user.id = :userId
            """)
    void softDeleteByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
                delete from tokens t
                where t.expiredAt < :now
            """)
    int deleteAllExpired(@Param("now") Instant now);

}
