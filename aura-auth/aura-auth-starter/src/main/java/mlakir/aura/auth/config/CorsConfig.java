package mlakir.aura.auth.config;

import java.util.*;

import lombok.*;
import org.springframework.boot.context.properties.*;

@Getter
@Setter
@ConfigurationProperties(prefix = "aura.security.cors")
public class CorsConfig {

    private List<String> allowedOrigins;

    private List<String> allowedMethods = List.of(
        "GET",
        "POST",
        "PUT",
        "DELETE",
        "OPTIONS",
        "PATCH");

    private List<String> allowedHeaders = List.of("*");

    private long maxAge = 3600;

}

