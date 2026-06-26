package mlakir.aura.auth.config;

import java.util.*;

import lombok.*;
import org.springframework.boot.context.properties.*;

@Getter
@Setter
@ConfigurationProperties(prefix = "aura.security.endpoints")
public class SecurityEndpointsConfig {

    private List<String> permitAll = new ArrayList<>(List.of(
        "/auth/login",
        "/auth/register",
        "/auth/refresh",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health"
    ));

}
