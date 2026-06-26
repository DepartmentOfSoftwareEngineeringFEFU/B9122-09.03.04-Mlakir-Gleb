package mlakir.aura.auth.validator;

import java.time.*;
import java.util.*;

import lombok.*;
import mlakir.aura.auth.*;
import mlakir.aura.auth.config.*;
import mlakir.aura.auth.entity.*;
import mlakir.aura.auth.repository.*;
import mlakir.aura.auth.utils.*;
import org.springframework.stereotype.*;

@Service
@RequiredArgsConstructor
public class JwtRefreshTokenValidator {

    private final JwtVerificationConfig config;

    private final TokenRepository tokenRepository;

    private final JwtParser jwtParser;

    private final JwtValidator jwtValidator;

    public boolean isValid(String token, UserEntity user) {

        if (!JwtTypes.REFRESH.equals(jwtParser.extractTokenType(token))) {
            return false;
        }

        if (!jwtParser.extractIssuer(token).equals(config.getJwtIssuer())) {
            return false;
        }

        if (jwtValidator.isTokenExpired(token)) {
            return false;
        }

        UUID refreshJti = jwtParser.extractJti(token);

        return tokenRepository.findByRefreshJti(refreshJti, Instant.now())
            .filter(t -> t.getUser().getId().equals(user.getId()))
            .isPresent();
    }

}
