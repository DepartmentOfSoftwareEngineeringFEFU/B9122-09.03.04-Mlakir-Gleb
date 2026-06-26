package mlakir.aura.auth;

import java.util.*;

import com.fasterxml.jackson.databind.*;
import mlakir.aura.auth.config.*;
import mlakir.aura.auth.filter.*;
import mlakir.aura.auth.handler.*;
import mlakir.aura.auth.handler.strategy.*;
import mlakir.aura.auth.utils.*;
import mlakir.aura.auth.validator.*;
import mlakir.aura.exception.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.security.servlet.*;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.*;
import org.springframework.security.core.*;
import org.springframework.security.web.access.*;
import org.springframework.web.cors.*;

@AutoConfiguration(before = {
    SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class
})
@EnableMethodSecurity
@EnableConfigurationProperties({
    JwtVerificationConfig.class,
    CorsConfig.class,
    SecurityEndpointsConfig.class
})
@Import(SecurityAutoConfig.class)
public class AuraAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuthenticationProvider.class)
    public AuthenticationProvider auraAuthFallbackAuthenticationProvider() {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication)
                throws AuthenticationException {
                return null;
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return false;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtVerificationKeyProvider jwtVerificationKeyProvider(JwtVerificationConfig config) {
        return new JwtVerificationKeyProvider(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtParser jwtParser(JwtVerificationKeyProvider keyProvider) {
        return new JwtParser(keyProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtValidator jwtValidator(JwtParser jwtParser) {
        return new JwtValidator(jwtParser);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAccessTokenValidator jwtAccessTokenValidator(
        JwtVerificationConfig config,
        JwtParser jwtParser,
        JwtValidator jwtValidator,
        JwtAccessTokenSessionValidator sessionValidator
    ) {
        return new JwtAccessTokenValidator(config, jwtParser, jwtValidator, sessionValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAccessTokenSessionValidator jwtAccessTokenSessionValidator() {
        return accessJti -> true;
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtFilter jwtFilter(
        JwtAccessTokenValidator validator,
        JwtAuthenticationEntryPoint entryPoint,
        JwtParser jwtParser
    ) {
        return new JwtFilter(validator, entryPoint, jwtParser);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProblemDetailResponseWriter problemDetailResponseWriter(
        ObjectMapper objectMapper,
        AuraExceptionHandler auraExceptionHandler
    ) {
        return new ProblemDetailResponseWriter(objectMapper, auraExceptionHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessDeniedHandler accessDeniedHandler(
        ProblemDetailResponseWriter problemDetailResponseWriter
    ) {
        return new AuraAccessDeniedHandler(problemDetailResponseWriter);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(
        ProblemDetailResponseWriter problemDetailResponseWriter
    ) {
        return new JwtAuthenticationEntryPoint(problemDetailResponseWriter);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtClaimProvider jwtClaimProvider(JwtParser jwtParser) {
        return new JwtClaimProvider(jwtParser);
    }

    @Bean
    public UnauthenticatedStrategy unauthenticatedDeniedStrategy() {
        return new UnauthenticatedStrategy();
    }

    @Bean
    public ForbiddenStrategy forbiddenDeniedStrategy() {
        return new ForbiddenStrategy();
    }

    @Bean
    public AuthorizationDeniedResolver authorizationDeniedResolver(
        List<AuthorizationDeniedStrategy> strategies
    ) {
        return new AuthorizationDeniedResolver(strategies);
    }

    @Bean
    public AuthExceptionHandler authenticationExceptionHandler(
        AuthorizationDeniedResolver resolver,
        AuraExceptionHandler auraExceptionHandler
    ) {
        return new AuthExceptionHandler(resolver, auraExceptionHandler);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsConfig props) {

        CorsConfiguration configuration = new CorsConfiguration();

        if (!props.getAllowedOrigins().isEmpty()) {
            configuration.setAllowedOrigins(props.getAllowedOrigins());
        }

        configuration.setAllowedMethods(props.getAllowedMethods());
        configuration.setAllowedHeaders(props.getAllowedHeaders());
        configuration.setMaxAge(props.getMaxAge());
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
