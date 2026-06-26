package mlakir.aura.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aura.bootstrap.admin")
public class AdminBootstrapProperties {

    private boolean enabled;

    private String login;

    private String password;
}
