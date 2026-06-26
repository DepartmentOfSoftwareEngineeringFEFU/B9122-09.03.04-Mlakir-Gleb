package mlakir.aura.auth.config;

import mlakir.aura.auth.provider.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.builders.*;
import org.springframework.security.config.annotation.web.builders.*;

@Configuration
public class AuthenticationConfig {

    @Bean
    public AuthenticationManager authenticationManager(
        HttpSecurity http,
        AuraAuthenticationProvider auraAuthenticationProvider
    ) throws Exception {

        AuthenticationManagerBuilder builder =
            http.getSharedObject(AuthenticationManagerBuilder.class);

        builder.authenticationProvider(auraAuthenticationProvider);

        return builder.build();
    }

}
