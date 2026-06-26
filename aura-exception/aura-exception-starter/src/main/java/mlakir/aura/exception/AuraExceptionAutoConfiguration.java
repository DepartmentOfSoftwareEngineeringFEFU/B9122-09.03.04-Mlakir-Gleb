package mlakir.aura.exception;

import jakarta.annotation.*;
import lombok.extern.slf4j.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.web.servlet.*;
import org.springframework.boot.context.properties.*;
import org.springframework.boot.web.servlet.error.*;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.mvc.method.annotation.*;

@AutoConfiguration
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({
    DispatcherServlet.class,
    ResponseEntityExceptionHandler.class,
    ErrorAttributes.class
})
@EnableConfigurationProperties(AuraExceptionProperties.class)
@Import(MessageSourceConfig.class)
public class AuraExceptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuraProblemDetailFactory auraProblemDetailFactory(AuraExceptionProperties properties) {
        return new AuraProblemDetailFactory(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuraExceptionHandler auraExceptionHandler(
        AuraProblemDetailFactory problemDetailFactory
    ) {
        return new AuraExceptionHandler(problemDetailFactory);
    }

}
