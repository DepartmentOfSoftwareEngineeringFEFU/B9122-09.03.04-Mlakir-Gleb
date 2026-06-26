package mlakir.aura.auth.config;

import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;

@Configuration
@EnableConfigurationProperties(JwtSigningConfig.class)
public class JwtConfig {

}
