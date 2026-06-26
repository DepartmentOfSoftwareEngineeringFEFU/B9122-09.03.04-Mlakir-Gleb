package mlakir.aura.auth.config;

import lombok.*;
import org.springframework.boot.context.properties.*;
import org.springframework.validation.annotation.*;

@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "aura.security.jwt.signing")
public class JwtSigningConfig {

    private String privateKey;

    private Long accessTokenExpirationSeconds;

    private Long refreshTokenExpirationSeconds;

    private String jwtIssuer;

    private String defaultRole;

}
