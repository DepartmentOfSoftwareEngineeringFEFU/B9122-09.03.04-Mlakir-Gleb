package mlakir.aura.auth.handler.strategy;

import java.util.*;

import lombok.*;
import org.springframework.security.core.*;

@RequiredArgsConstructor
public class AuthorizationDeniedResolver {

    private final List<AuthorizationDeniedStrategy> strategies;

    public AuthorizationDeniedStrategy resolve(Authentication auth) {
        return strategies.stream()
            .filter(s -> s.supports(auth))
            .findFirst()
            .orElse(new ForbiddenStrategy());
    }

}
