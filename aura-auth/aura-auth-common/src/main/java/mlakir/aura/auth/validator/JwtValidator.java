package mlakir.aura.auth.validator;

import java.time.*;

import lombok.*;
import mlakir.aura.auth.utils.*;

@RequiredArgsConstructor
public class JwtValidator {

    private final JwtParser jwtParser;

    public boolean isTokenExpired(String token) {
        return jwtParser.extractExpiration(token).isBefore(Instant.now());
    }

}
