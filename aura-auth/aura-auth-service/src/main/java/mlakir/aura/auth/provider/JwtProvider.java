package mlakir.aura.auth.provider;

import java.time.*;
import java.util.*;

import io.jsonwebtoken.*;
import lombok.*;
import mlakir.aura.auth.*;
import mlakir.aura.auth.config.*;
import mlakir.aura.auth.entity.*;
import org.springframework.security.core.*;
import org.springframework.stereotype.*;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtSigningConfig config;

    private final JwtSigningKeyProvider keyProvider;

    public String generateAccessToken(UserEntity userEntity, UUID accessJti) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
            .id(accessJti.toString())
            .subject(userEntity.getLogin())
            .issuer(config.getJwtIssuer())
            .claim("user_id", userEntity.getId())
            .claim("token_type", JwtTypes.ACCESS)
            .claim(
                "authorities",
                userEntity.getRoles().stream().map(GrantedAuthority::getAuthority).toList())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(config.getAccessTokenExpirationSeconds())))
            .signWith(keyProvider.getPrivateKey());
        return builder.compact();
    }

    public String generateRefreshToken(UserEntity userEntity, UUID refreshJti) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
            .id(refreshJti.toString())
            .subject(userEntity.getLogin())
            .issuer(config.getJwtIssuer())
            .claim("user_id", userEntity.getId())
            .claim("token_type", JwtTypes.REFRESH)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(config.getRefreshTokenExpirationSeconds())))
            .signWith(keyProvider.getPrivateKey());
        return builder.compact();
    }

}
