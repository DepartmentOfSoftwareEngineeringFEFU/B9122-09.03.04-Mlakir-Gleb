package mlakir.aura.auth.validator;

import java.util.*;

import lombok.*;
import mlakir.aura.auth.*;
import mlakir.aura.auth.config.*;
import mlakir.aura.auth.utils.*;

@RequiredArgsConstructor
public class JwtAccessTokenValidator {

    private final JwtVerificationConfig properties;

    private final JwtParser jwtParser;

    private final JwtValidator jwtValidator;

    private final JwtAccessTokenSessionValidator sessionValidator;

    public boolean isValid(String token) {

        if (!JwtTypes.ACCESS.equals(jwtParser.extractTokenType(token))) {
            return false;
        }

        if (!jwtParser.extractIssuer(token).equals(properties.getJwtIssuer())) {
            return false;
        }

        if (jwtValidator.isTokenExpired(token)) {
            return false;
        }

        return sessionValidator.isActive(jwtParser.extractJti(token));
    }

}
