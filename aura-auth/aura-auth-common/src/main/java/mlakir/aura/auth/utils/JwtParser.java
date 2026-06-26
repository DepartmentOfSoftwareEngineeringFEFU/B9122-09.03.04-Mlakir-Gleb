package mlakir.aura.auth.utils;

import java.time.*;
import java.util.*;
import java.util.function.*;

import io.jsonwebtoken.*;
import lombok.*;
import mlakir.aura.auth.*;
import org.springframework.security.core.authority.*;

@RequiredArgsConstructor
public class JwtParser {

    private final JwtVerificationKeyProvider keyProvider;

    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    public String extractIssuer(String token) {
        return extractClaim(token, Claims::getIssuer);
    }

    public String extractLogin(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractJti(String token) {
        return extractClaim(token, claims -> UUID.fromString(claims.getId()));
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("user_id", Long.class));
    }

    public JwtTypes extractTokenType(String token) {
        return extractClaim(
            token,
            claims -> JwtTypes.valueOf(claims.get("token_type", String.class)));
    }

    public List<SimpleGrantedAuthority> extractAuthorities(String token) {
        List<?> authorities = extractClaim(
            token, claims ->
                claims.get("authorities", List.class));

        if (authorities == null) {
            return Collections.emptyList();
        }

        return authorities.stream()
            .filter(String.class::isInstance)
            .map(authority -> new SimpleGrantedAuthority((String) authority))
            .toList();
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        JwtParserBuilder parser = Jwts.parser();
        parser.verifyWith(keyProvider.publicKey());
        try {
            return parser.build().parseSignedClaims(token).getPayload();
        } catch (Exception ex) {
            throw AuthExceptionFabric.createInvalidToken();
        }
    }

}
