package mlakir.aura.auth.validator;

import java.time.*;
import java.util.*;

import lombok.*;
import mlakir.aura.auth.repository.*;
import org.springframework.stereotype.*;

@Component
@RequiredArgsConstructor
public class DbJwtAccessTokenSessionValidator implements JwtAccessTokenSessionValidator {

    private final TokenRepository tokenRepository;

    @Override
    public boolean isActive(UUID accessJti) {
        return tokenRepository.findByAccessJti(accessJti, Instant.now()).isPresent();
    }

}
