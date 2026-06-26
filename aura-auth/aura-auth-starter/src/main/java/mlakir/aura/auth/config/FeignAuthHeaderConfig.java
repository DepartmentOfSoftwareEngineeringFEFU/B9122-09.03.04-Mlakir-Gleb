package mlakir.aura.auth.config;

import feign.*;
import jakarta.servlet.http.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.web.context.request.*;

@AutoConfiguration
public class FeignAuthHeaderConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Bean
    public RequestInterceptor jwtForwardingInterceptor() {
        return template -> {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
                return;
            }

            HttpServletRequest request = servletRequestAttributes.getRequest();
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);

            if (authHeader != null && !authHeader.isBlank()) {
                template.header(AUTHORIZATION_HEADER, authHeader);
            }
        };
    }

}
