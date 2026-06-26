package mlakir.aura.auth.mapper;

import java.time.*;
import java.util.*;

import mlakir.aura.auth.dto.*;
import mlakir.aura.auth.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", source = "defaultRole", qualifiedByName = "toRoleSet")
    UserEntity toUserEntity(RegisterRequestDto request, RoleEntity defaultRole);

    @Mapping(target = "id", ignore = true)
    CredentialEntity toCredentialEntity(UserEntity user, String salt, String hash);

    @Mapping(target = "id", ignore = true)
    TokenEntity toTokenEntity(
        UUID accessJti,
        UUID refreshJti,
        UserEntity user,
        Instant issuedAt,
        Instant expiredAt
    );

    AuthResponseDto toAuthResponseDto(String accessToken, String refreshToken);

    @Named("toRoleSet")
    default Set<RoleEntity> toRoleSet(RoleEntity role) {
        return new HashSet<>(Set.of(role));
    }

}
