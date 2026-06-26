package mlakir.aura.auth.handler;

import jakarta.servlet.http.*;
import mlakir.aura.exception.*;
import mlakir.aura.auth.handler.strategy.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthExceptionHandlerTest {

    @Mock
    private AuthorizationDeniedResolver resolver;

    @Mock
    private AuraExceptionHandler auraExceptionHandler;

    @Mock
    private HttpServletRequest request;

    @Test
    void badCredentialsReturnsUnauthorizedProblem() {
        AuthExceptionHandler handler =
            new AuthExceptionHandler(resolver, auraExceptionHandler);
        ProblemDetail expected = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        expected.setTitle("Invalid credentials");

        when(auraExceptionHandler.handleAuraException(any(AuraException.class), eq(request)))
            .thenReturn(expected);

        ProblemDetail detail = handler.handleBadCredentials(
            new BadCredentialsException("bad credentials"),
            request);

        assertSame(expected, detail);
        assertEquals("Invalid credentials", detail.getTitle());
    }
}
