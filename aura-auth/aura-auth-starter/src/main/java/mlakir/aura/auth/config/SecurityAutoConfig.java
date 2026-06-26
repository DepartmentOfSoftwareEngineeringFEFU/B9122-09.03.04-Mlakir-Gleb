package mlakir.aura.auth.config;

import lombok.*;
import mlakir.aura.auth.filter.*;
import mlakir.aura.auth.handler.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configurers.*;
import org.springframework.security.config.http.*;
import org.springframework.security.web.*;
import org.springframework.security.web.access.*;
import org.springframework.security.web.authentication.*;

@Configuration
@RequiredArgsConstructor
public class SecurityAutoConfig {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtFilter jwtFilter,
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
        AccessDeniedHandler accessDeniedHandler,
        SecurityEndpointsConfig endpointsConfig
    ) throws Exception {

        http.cors(Customizer.withDefaults());

        http.csrf(AbstractHttpConfigurer::disable);

        http.sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(endpointsConfig.getPermitAll().toArray(String[]::new))
            .permitAll()
            .anyRequest().authenticated());

        http.exceptionHandling(ex -> {
            ex.authenticationEntryPoint(jwtAuthenticationEntryPoint);
            ex.accessDeniedHandler(accessDeniedHandler);
        });

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
