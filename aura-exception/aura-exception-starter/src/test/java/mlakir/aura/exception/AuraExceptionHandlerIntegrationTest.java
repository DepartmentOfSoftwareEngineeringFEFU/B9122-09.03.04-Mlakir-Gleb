package mlakir.aura.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.http.*;
import org.springframework.boot.autoconfigure.jackson.*;
import org.springframework.boot.autoconfigure.validation.*;
import org.springframework.boot.autoconfigure.web.servlet.*;
import org.springframework.boot.autoconfigure.web.servlet.error.*;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.test.web.servlet.*;
import org.springframework.web.bind.annotation.*;

@SpringBootTest(
    classes = AuraExceptionHandlerIntegrationTest.TestApplication.class,
    properties = "mlakir.aura.exception.include-exception-message=false"
)
@AutoConfigureMockMvc
class AuraExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldHandleAuraException() throws Exception {
        mockMvc.perform(get("/test/aura"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Conflict"))
            .andExpect(jsonPath("$.detail").value("Business rule failed"))
            .andExpect(jsonPath("$.instance").value("/test/aura"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldHandleValidationException() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation error"))
            .andExpect(jsonPath("$.detail").value("Ошибка в одном или нескольких полях."))
            .andExpect(jsonPath("$.errors.name").value("Поле обязательно для заполнения"));
    }

    @Test
    void shouldHandleMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/test/auth"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Missing token"))
            .andExpect(jsonPath("$.detail").value("Пользователь не аутентифицирован."))
            .andExpect(jsonPath("$.instance").value("/test/auth"));
    }

    @Test
    void shouldHandleIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.detail").value(
                "Запрос некорректен, поскольку выбранные параметры "
                    + "указаны неверно или произошла функциональная ошибка."
            ))
            .andExpect(jsonPath("$.message").value("bad input"))
            .andExpect(jsonPath("$.instance").value("/test/illegal-argument"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldHideInternalMessageByDefault() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.title").value("Internal Server Error"))
            .andExpect(jsonPath("$.detail").value("Произошла неожиданная ошибка."))
            .andExpect(jsonPath("$.errorId").exists())
            .andExpect(jsonPath("$.message").doesNotExist())
            .andExpect(jsonPath("$.instance").value("/test/unexpected"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = ErrorMvcAutoConfiguration.class)
    @Import(TestController.class)
    @ImportAutoConfiguration({
        JacksonAutoConfiguration.class,
        ValidationAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        WebMvcAutoConfiguration.class,
        AuraExceptionAutoConfiguration.class
    })
    static class TestApplication {
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/aura")
        String aura() {
            ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Business rule failed");
            throw new AuraException(HttpStatus.CONFLICT, problemDetail);
        }

        @PostMapping("/validation")
        String validation(@Valid @RequestBody TestRequest request) {
            return request.name();
        }

        @GetMapping("/auth")
        String auth(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
            return authorization;
        }

        @GetMapping("/unexpected")
        String unexpected() {
            throw new IllegalStateException("secret internal state");
        }

        @GetMapping("/illegal-argument")
        String illegalArgument() {
            throw new IllegalArgumentException("bad input");
        }
    }

    record TestRequest(@NotBlank String name) {
    }

}
