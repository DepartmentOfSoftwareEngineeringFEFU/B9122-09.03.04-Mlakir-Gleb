package mlakir.aura.auth.service;

import java.util.*;

import mlakir.aura.auth.*;
import mlakir.aura.auth.config.*;
import mlakir.aura.auth.dto.*;
import mlakir.aura.auth.provider.*;
import mlakir.aura.auth.repository.*;
import mlakir.aura.auth.utils.*;
import mlakir.aura.auth.validator.*;
import mlakir.aura.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtSigningConfig config;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtParser jwtParser;

    @Mock
    private JwtRefreshTokenValidator refreshTokenValidator;

    @Mock
    private JwtAccessTokenValidator accessTokenValidator;

    @InjectMocks
    private AuthService authService;

    @Test
    void logoutRejectsTokenOfAnotherUser() {
        Principal principal = new Principal(7L);
        SecurityContext context = mock(SecurityContext.class);

        when(accessTokenValidator.isValid("access-token")).thenReturn(true);
        when(jwtParser.extractUserId("access-token")).thenReturn(42L);
        when(context.getAuthentication()).thenReturn(
            new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        SecurityContextHolder.setContext(context);

        AuraException exception = assertThrows(
            AuraException.class,
            () -> authService.logout("Bearer access-token"));

        assertEquals(403, exception.getStatusCode().value());
        verify(tokenRepository, never()).softDeleteByAccessJti(any(), any());
    }

    @Test
    void registerRejectsDuplicateLogin() {
        RegisterRequestDto request = new RegisterRequestDto("gleb", "secret");

        when(userRepository.existsByLogin("gleb")).thenReturn(true);

        AuraException exception = assertThrows(
            AuraException.class,
            () -> authService.register(request));

        assertEquals(409, exception.getStatusCode().value());
        verify(roleRepository, never()).findByCode(anyString());
        verify(credentialRepository, never()).save(any());
    }

    @Test
    void logoutAllInvalidatesAllUserTokens() {
        Principal principal = new Principal(7L);
        SecurityContext context = mock(SecurityContext.class);

        when(context.getAuthentication()).thenReturn(
            new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        SecurityContextHolder.setContext(context);

        authService.logoutAll();

        verify(tokenRepository).softDeleteByUserId(eq(7L), any());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
