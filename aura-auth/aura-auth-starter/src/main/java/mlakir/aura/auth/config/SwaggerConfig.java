package mlakir.aura.auth.config;

import java.util.*;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;
import org.springframework.core.io.*;

@AutoConfiguration
public class SwaggerConfig {

    public static final String SECURITY_SCHEME_NAME = "Bearer Authorization";

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.application.description:}")
    private String appDescription;

    @Value("${spring.application.version}")
    private String appVersion;

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI customOpenApi(
        Properties gitProps
    ) {
        return new OpenAPI()
            .info(new Info()
                .title(appName)
                .version(appVersion)
                .description(buildDescription(gitProps)))
            .addSecurityItem(new SecurityRequirement()
                .addList(SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME, new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }

    @Bean
    public Properties gitProps(
        ResourceLoader loader
    ) {
        var props = new Properties();

        var resource = loader
            .getResource("classpath:git.properties");
        if (!resource.exists()) {
            return props;
        }
        try (var is = resource.getInputStream()) {
            props.load(is);
            return props;
        } catch (Exception ex) {
            return props;
        }
    }

    private String buildDescription(
        Properties gitProps
    ) {
        var description = new StringBuilder(
            Optional.ofNullable(appDescription).orElse(""));

        var branch = gitProps.getProperty("git.branch");
        var buildTime = gitProps.getProperty("git.build.time");
        var commitId = gitProps.getProperty("git.commit.id");
        if (branch == null && buildTime == null && commitId == null) {
            return description.toString();
        }

        if (!description.isEmpty()) {
            description.append("<br/>");
        }
        description.append("""
            **Build Information:**
            - Branch: %s
            - Build Time: %s
            - Commit: %s
            """.formatted(
            Optional.ofNullable(branch).orElse("unknown"),
            Optional.ofNullable(buildTime).orElse("unknown"),
            Optional.ofNullable(commitId).orElse("unknown")));
        return description.toString();
    }

}
