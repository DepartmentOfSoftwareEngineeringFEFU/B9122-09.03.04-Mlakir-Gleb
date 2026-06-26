package mlakir.aura.exception;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.validation.*;
import org.springframework.boot.test.context.runner.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;

class AuraExceptionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                ValidationAutoConfiguration.class,
                AuraExceptionAutoConfiguration.class
            )
        );

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                ValidationAutoConfiguration.class,
                AuraExceptionAutoConfiguration.class
            )
        );

    @Test
    void shouldNotLoadInNonWebApplication() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AuraExceptionHandler.class);
        });
    }

    @Test
    void shouldLoadDefaultBeansInServletApplication() {
        webContextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuraExceptionHandler.class);
            assertThat(context).hasSingleBean(AuraProblemDetailFactory.class);
            assertThat(context).hasSingleBean(AuraExceptionProperties.class);
        });
    }

    @Test
    void shouldAllowProblemDetailFactoryOverride() {
        webContextRunner
            .withUserConfiguration(CustomProblemDetailFactoryConfiguration.class)
            .run(context -> {
                AuraProblemDetailFactory factory = context.getBean(AuraProblemDetailFactory.class);
                ProblemDetail problemDetail = factory.buildIllegalArgumentError(new IllegalArgumentException("boom"));

                assertThat(problemDetail.getDetail()).isEqualTo("custom");
            });
    }

    @Test
    void shouldBindIllegalArgumentProperties() {
        webContextRunner
            .withPropertyValues(
                "mlakir.aura.exception.illegal-argument-title=Custom bad request",
                "mlakir.aura.exception.illegal-argument-detail=Custom invalid input"
            )
            .run(context -> {
                AuraExceptionProperties properties = context.getBean(AuraExceptionProperties.class);

                assertThat(properties.getIllegalArgumentTitle()).isEqualTo("Custom bad request");
                assertThat(properties.getIllegalArgumentDetail()).isEqualTo("Custom invalid input");
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomProblemDetailFactoryConfiguration {

        @Bean
        AuraProblemDetailFactory auraProblemDetailFactory(AuraExceptionProperties properties) {
            return new AuraProblemDetailFactory(properties) {
                @Override
                public ProblemDetail buildIllegalArgumentError(IllegalArgumentException ex) {
                    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "custom");
                }
            };
        }
    }

}
