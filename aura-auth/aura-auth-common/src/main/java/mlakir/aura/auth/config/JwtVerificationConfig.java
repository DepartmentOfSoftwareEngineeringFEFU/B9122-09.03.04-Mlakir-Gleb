package mlakir.aura.auth.config;

import lombok.*;
import org.springframework.boot.context.properties.*;

@Getter
@Setter
@AllArgsConstructor
@ConfigurationProperties(prefix = "aura.security.jwt.verification")
public class JwtVerificationConfig {

    private String publicKey;

    private String jwtIssuer;

}
